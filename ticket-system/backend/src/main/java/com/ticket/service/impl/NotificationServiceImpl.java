package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.constant.BusinessConstants;
import com.ticket.dto.response.NotificationResponse;
import com.ticket.entity.Notification;
import com.ticket.event.TicketAssignedEvent;
import com.ticket.event.TicketCreatedEvent;
import com.ticket.event.TicketOverdueEvent;
import com.ticket.mapper.NotificationMapper;
import com.ticket.mapper.UserMapper;
import com.ticket.service.NotificationService;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedissonClient redissonClient;

    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   UserMapper userMapper,
                                   SimpMessagingTemplate messagingTemplate,
                                   RedissonClient redissonClient) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.messagingTemplate = messagingTemplate;
        this.redissonClient = redissonClient;
    }

    // ── Event-driven: create + push ──

    @Transactional
    public void handleTicketCreated(TicketCreatedEvent event) {
        // Notify all agents and admins
        var recipients = userMapper.selectList(new LambdaQueryWrapper<>())
                .stream()
                .filter(u -> "ROLE_AGENT".equals(u.getRole()) || "ROLE_ADMIN".equals(u.getRole()))
                .toList();

        for (var user : recipients) {
            Notification notif = buildNotification(user.getId(),
                    BusinessConstants.NOTIF_TICKET_CREATED, event.getTicketId(),
                    "New ticket: " + event.getTitle(), null);
            pushAndSave(notif);
        }
    }

    @Transactional
    public void handleTicketAssigned(TicketAssignedEvent event) {
        Notification notif = buildNotification(event.getAssignedTo(),
                BusinessConstants.NOTIF_TICKET_ASSIGNED, event.getTicketId(),
                "Ticket #" + event.getTicketId() + " assigned to you", null);
        pushAndSave(notif);
    }

    @Transactional
    public void handleTicketOverdue(TicketOverdueEvent event) {
        // Notify the assigned agent (if assigned), otherwise notify all agents
        if (event.getAssignedTo() != null) {
            Notification notif = buildNotification(event.getAssignedTo(),
                    BusinessConstants.NOTIF_TICKET_OVERDUE, event.getTicketId(),
                    "Ticket #" + event.getTicketId() + " is overdue (" + event.getOverdueMinutes() + "min)",
                    "Priority: " + event.getPriority() + " | Type: " + event.getType());
            pushAndSave(notif);
        } else {
            // Unassigned overdue: notify all agents and admins
            var recipients = userMapper.selectList(new LambdaQueryWrapper<>())
                    .stream()
                    .filter(u -> "ROLE_AGENT".equals(u.getRole()) || "ROLE_ADMIN".equals(u.getRole()))
                    .toList();
            for (var user : recipients) {
                Notification notif = buildNotification(user.getId(),
                        BusinessConstants.NOTIF_TICKET_OVERDUE, event.getTicketId(),
                        "Unassigned ticket #" + event.getTicketId() + " is overdue (" + event.getOverdueMinutes() + "min)",
                        "Priority: " + event.getPriority());
                pushAndSave(notif);
            }
        }
    }

    private Notification buildNotification(Long userId, String type, Long ticketId, String title, String message) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTicketId(ticketId);
        n.setTitle(title);
        n.setMessage(message);
        n.setIsRead(0);
        n.setCreatedDate(System.currentTimeMillis());
        return n;
    }

    private void pushAndSave(Notification notif) {
        notificationMapper.insert(notif);
        // Increment Redis unread counter
        redissonClient.getAtomicLong("unread:count:" + notif.getUserId()).incrementAndGet();
        // Push via WebSocket to the target user
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(notif.getUserId()), "/queue/notifications", toResponse(notif));
        } catch (Exception e) {
            log.warn("WebSocket push failed for userId={}: {}", notif.getUserId(), e.getMessage());
        }
    }

    // ── REST: read operations ──

    @Override
    public List<NotificationResponse> listNotifications(Long userId, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedDate);
        return notificationMapper.selectPage(Page.of(page, size), wrapper)
                .getRecords().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(Long userId) {
        RAtomicLong counter = redissonClient.getAtomicLong("unread:count:" + userId);
        if (counter.isExists()) {
            return counter.get();
        }
        // Fallback: count from DB and sync to Redis
        long count = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        counter.set(count);
        return count;
    }

    @Override
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification n = notificationMapper.selectById(notificationId);
        if (n == null || !n.getUserId().equals(userId)) return;

        if (n.getIsRead() == 0) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
            redissonClient.getAtomicLong("unread:count:" + userId).decrementAndGet();
        }
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1);
        notificationMapper.update(null, wrapper);
        redissonClient.getAtomicLong("unread:count:" + userId).set(0);
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setType(n.getType());
        r.setTicketId(n.getTicketId());
        r.setTitle(n.getTitle());
        r.setMessage(n.getMessage());
        r.setIsRead(n.getIsRead() == 1);
        r.setCreatedDate(n.getCreatedDate());
        return r;
    }
}
