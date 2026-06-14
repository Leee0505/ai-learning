package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.constant.BusinessConstants;
import com.ticket.common.constant.ErrorCode;
import com.ticket.common.constant.RoleConstants;
import com.ticket.common.exception.BusinessException;
import com.ticket.common.exception.TicketAccessDeniedException;
import com.ticket.common.exception.TicketNotFoundException;
import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import com.ticket.entity.*;
import com.ticket.mapper.*;
import com.ticket.service.TicketService;
import com.ticket.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketServiceImpl implements TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);

    private static final Set<String> VALID_STATUSES = Set.of(
            BusinessConstants.TICKET_STATUS_OPEN,
            BusinessConstants.TICKET_STATUS_IN_PROGRESS,
            BusinessConstants.TICKET_STATUS_RESOLVED,
            BusinessConstants.TICKET_STATUS_CLOSED);

    private static final Set<String> VALID_PRIORITIES = Set.of(
            BusinessConstants.TICKET_PRIORITY_LOW,
            BusinessConstants.TICKET_PRIORITY_MEDIUM,
            BusinessConstants.TICKET_PRIORITY_HIGH,
            BusinessConstants.TICKET_PRIORITY_URGENT);

    private static final Set<String> VALID_CATEGORIES = Set.of(
            BusinessConstants.TICKET_CATEGORY_BUG,
            BusinessConstants.TICKET_CATEGORY_FEATURE_REQUEST,
            BusinessConstants.TICKET_CATEGORY_GENERAL_QUESTION,
            BusinessConstants.TICKET_CATEGORY_ACCOUNT_ISSUE,
            BusinessConstants.TICKET_CATEGORY_OTHER);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "json", "xml", "log", "md",
            "zip", "rar", "7z", "tar", "gz",
            "mp4", "mov", "webm");

    private final TicketMapper ticketMapper;
    private final TicketReplyMapper ticketReplyMapper;
    private final TicketAttachmentMapper ticketAttachmentMapper;
    private final UserMapper userMapper;
    private final FileStorageService fileStorage;

    public TicketServiceImpl(TicketMapper ticketMapper, TicketReplyMapper ticketReplyMapper,
                             TicketAttachmentMapper ticketAttachmentMapper, UserMapper userMapper,
                             FileStorageService fileStorage) {
        this.ticketMapper = ticketMapper;
        this.ticketReplyMapper = ticketReplyMapper;
        this.ticketAttachmentMapper = ticketAttachmentMapper;
        this.userMapper = userMapper;
        this.fileStorage = fileStorage;
    }

    // ──────────────────────────────────────────────
    //  Create
    // ──────────────────────────────────────────────

    @Override
    @Transactional
    public TicketDetailResponse createTicket(CreateTicketRequest request, Long userId) {
        validatePriority(request.getPriority());
        validateCategory(request.getCategory());

        Ticket ticket = new Ticket();
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority());
        ticket.setCategory(request.getCategory());
        ticket.setStatus(BusinessConstants.TICKET_STATUS_OPEN);
        ticket.setCreatedBy(userId); // Explicit owner; MetaObjectHandler fallback
        ticketMapper.insert(ticket);

        TicketDetailResponse response = TicketDetailResponse.from(ticket);
        String creatorName = getUsername(userId);
        response.setCreatedByName(creatorName);
        response.setReplies(Collections.emptyList());
        response.setAttachments(Collections.emptyList());

        log.info("Ticket created: id={} by userId={}", ticket.getId(), userId);
        return response;
    }

    // ──────────────────────────────────────────────
    //  List
    // ──────────────────────────────────────────────

    @Override
    public PageResponse<TicketResponse> listTickets(String status, String priority, String category,
                                                     String keyword, int pageNum, int size,
                                                     Long userId, String role, String sortOrder) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();

        // Role-based visibility: ROLE_USER sees only own tickets
        if (RoleConstants.ROLE_USER.equals(role)) {
            wrapper.eq(Ticket::getCreatedBy, userId);
        }

        // Filters
        if (status != null && !status.isBlank()) {
            wrapper.eq(Ticket::getStatus, status);
        }
        if (priority != null && !priority.isBlank()) {
            wrapper.eq(Ticket::getPriority, priority);
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq(Ticket::getCategory, category);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Ticket::getTitle, keyword);
        }
        if ("asc".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(Ticket::getCreatedDate);
        } else {
            wrapper.orderByDesc(Ticket::getCreatedDate);
        }

        Page<Ticket> pageResult = ticketMapper.selectPage(
                new Page<>(pageNum, size), wrapper);

        // Batch-load usernames
        Set<Long> userIds = new HashSet<>();
        for (Ticket ticket : pageResult.getRecords()) {
            userIds.add(ticket.getCreatedBy());
            if (ticket.getAssignedTo() != null) {
                userIds.add(ticket.getAssignedTo());
            }
        }
        Map<Long, String> usernameMap = getUsernameMap(userIds);

        List<TicketResponse> records = pageResult.getRecords().stream().map(ticket -> {
            TicketResponse r = TicketResponse.from(ticket);
            r.setCreatedByName(usernameMap.getOrDefault(ticket.getCreatedBy(), "Unknown"));
            if (ticket.getAssignedTo() != null) {
                r.setAssignedToName(usernameMap.getOrDefault(ticket.getAssignedTo(), "Unknown"));
            }
            return r;
        }).collect(Collectors.toList());

        return PageResponse.of(pageResult, records);
    }

    // ──────────────────────────────────────────────
    //  Detail
    // ──────────────────────────────────────────────

    @Override
    public TicketDetailResponse getTicketDetail(Long ticketId, Long userId, String role) {
        Ticket ticket = findTicketOrFail(ticketId);
        checkTicketAccess(ticket, userId, role);

        TicketDetailResponse response = TicketDetailResponse.from(ticket);
        response.setCreatedByName(getUsername(ticket.getCreatedBy()));
        if (ticket.getAssignedTo() != null) {
            response.setAssignedToName(getUsername(ticket.getAssignedTo()));
        }

        // Load replies (filter internal notes for ROLE_USER)
        LambdaQueryWrapper<TicketReply> replyWrapper = new LambdaQueryWrapper<TicketReply>()
                .eq(TicketReply::getTicketId, ticketId)
                .orderByAsc(TicketReply::getCreatedDate);
        List<TicketReply> replies = ticketReplyMapper.selectList(replyWrapper);

        // Collect user IDs for batch username lookup
        Set<Long> replyUserIds = replies.stream()
                .map(TicketReply::getUserId)
                .collect(Collectors.toSet());
        Map<Long, String> replyUsernameMap = getUsernameMap(replyUserIds);

        List<TicketReplyResponse> replyResponses = replies.stream()
                .filter(r -> !RoleConstants.ROLE_USER.equals(role) || r.getIsInternal() == 0)
                .map(r -> {
                    TicketReplyResponse rr = TicketReplyResponse.from(r);
                    rr.setUsername(replyUsernameMap.getOrDefault(r.getUserId(), "Unknown"));
                    return rr;
                })
                .collect(Collectors.toList());
        response.setReplies(replyResponses);

        // Load attachments
        LambdaQueryWrapper<TicketAttachment> attachWrapper = new LambdaQueryWrapper<TicketAttachment>()
                .eq(TicketAttachment::getTicketId, ticketId)
                .orderByAsc(TicketAttachment::getCreatedDate);
        List<TicketAttachment> attachments = ticketAttachmentMapper.selectList(attachWrapper);
        List<TicketAttachmentResponse> attachResponses = attachments.stream()
                .map(TicketAttachmentResponse::from)
                .collect(Collectors.toList());
        response.setAttachments(attachResponses);

        return response;
    }

    // ──────────────────────────────────────────────
    //  Update
    // ──────────────────────────────────────────────

    @Override
    @Transactional
    public TicketDetailResponse updateTicket(Long ticketId, UpdateTicketRequest request,
                                              Long userId, String role) {
        Ticket ticket = findTicketOrFail(ticketId);
        checkTicketAccess(ticket, userId, role);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            ticket.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            ticket.setDescription(request.getDescription());
        }
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            validatePriority(request.getPriority());
            ticket.setPriority(request.getPriority());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            validateCategory(request.getCategory());
            ticket.setCategory(request.getCategory());
        }

        ticketMapper.updateById(ticket);
        log.info("Ticket updated: id={} by userId={}", ticketId, userId);
        return getTicketDetail(ticketId, userId, role);
    }

    // ──────────────────────────────────────────────
    //  Delete
    // ──────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteTicket(Long ticketId) {
        findTicketOrFail(ticketId);

        // Clean up attachment files via storage backend before cascade deletes DB rows
        LambdaQueryWrapper<TicketAttachment> attachWrapper = new LambdaQueryWrapper<TicketAttachment>()
                .eq(TicketAttachment::getTicketId, ticketId);
        List<TicketAttachment> attachments = ticketAttachmentMapper.selectList(attachWrapper);
        for (TicketAttachment att : attachments) {
            fileStorage.delete(att.getStoragePath());
        }

        ticketMapper.deleteById(ticketId);
        log.info("Ticket deleted: id={}", ticketId);
    }

    // ──────────────────────────────────────────────
    //  Change Status
    // ──────────────────────────────────────────────

    @Override
    @Transactional
    public TicketDetailResponse changeStatus(Long ticketId, ChangeStatusRequest request,
                                              Long userId, String role) {
        Ticket ticket = findTicketOrFail(ticketId);
        String newStatus = request.getStatus();

        if (!VALID_STATUSES.contains(newStatus)) {
            throw new BusinessException(ErrorCode.TICKET_STATUS_INVALID);
        }

        String current = ticket.getStatus();
        // Valid transitions: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED, OPEN -> CLOSED
        boolean valid = BusinessConstants.TICKET_STATUS_OPEN.equals(current)
                && (BusinessConstants.TICKET_STATUS_IN_PROGRESS.equals(newStatus)
                    || BusinessConstants.TICKET_STATUS_CLOSED.equals(newStatus))
                || BusinessConstants.TICKET_STATUS_IN_PROGRESS.equals(current)
                && (BusinessConstants.TICKET_STATUS_RESOLVED.equals(newStatus)
                    || BusinessConstants.TICKET_STATUS_CLOSED.equals(newStatus))
                || BusinessConstants.TICKET_STATUS_RESOLVED.equals(current)
                && BusinessConstants.TICKET_STATUS_CLOSED.equals(newStatus);

        if (!valid) {
            throw new BusinessException(ErrorCode.TICKET_STATUS_INVALID);
        }

        long now = System.currentTimeMillis();
        ticket.setStatus(newStatus);
        if (BusinessConstants.TICKET_STATUS_RESOLVED.equals(newStatus)) {
            ticket.setResolvedDate(now);
        }
        if (BusinessConstants.TICKET_STATUS_CLOSED.equals(newStatus)) {
            ticket.setClosedDate(now);
        }
        ticketMapper.updateById(ticket);

        log.info("Ticket status changed: id={} {} -> {} by userId={}", ticketId, current, newStatus, userId);
        return getTicketDetail(ticketId, userId, role);
    }

    // ──────────────────────────────────────────────
    //  Assign
    // ──────────────────────────────────────────────

    @Override
    @Transactional
    public TicketDetailResponse assignTicket(Long ticketId, AssignTicketRequest request,
                                              Long userId, String role) {
        Ticket ticket = findTicketOrFail(ticketId);
        Long targetId = request.getAssignedTo();

        // Verify target user exists and is an agent
        User targetUser = userMapper.selectById(targetId);
        if (targetUser == null || !RoleConstants.ROLE_AGENT.equals(targetUser.getRole())) {
            throw new BusinessException(ErrorCode.TICKET_ASSIGN_INVALID);
        }

        ticket.setAssignedTo(targetId);
        ticketMapper.updateById(ticket);

        log.info("Ticket assigned: id={} to agentId={} by userId={}", ticketId, targetId, userId);
        return getTicketDetail(ticketId, userId, role);
    }

    // ──────────────────────────────────────────────
    //  Reply
    // ──────────────────────────────────────────────

    @Override
    @Transactional
    public TicketReplyResponse addReply(Long ticketId, CreateReplyRequest request, Long userId) {
        findTicketOrFail(ticketId);

        TicketReply reply = new TicketReply();
        reply.setTicketId(ticketId);
        reply.setUserId(userId);
        reply.setContent(request.getContent());
        reply.setIsInternal(request.getIsInternal() ? 1 : 0);
        ticketReplyMapper.insert(reply);

        TicketReplyResponse response = TicketReplyResponse.from(reply);
        response.setUsername(getUsername(userId));

        log.info("Reply added: ticketId={} by userId={} internal={}", ticketId, userId, request.getIsInternal());
        return response;
    }

    // ──────────────────────────────────────────────
    //  Upload Attachment
    // ──────────────────────────────────────────────

    @Override
    @Transactional
    public TicketAttachmentResponse uploadAttachment(Long ticketId, MultipartFile file, Long userId) {
        findTicketOrFail(ticketId);

        // Validate size
        if (file.getSize() > BusinessConstants.MAX_UPLOAD_SIZE) {
            throw new BusinessException(ErrorCode.TICKET_ATTACHMENT_TOO_LARGE);
        }

        // Validate extension
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.TICKET_ATTACHMENT_TYPE_DENIED);
        }

        // Delegate file storage to the active backend (local / MinIO / OSS)
        String storageKey;
        try {
            storageKey = fileStorage.store(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to store file");
        }

        // Create attachment entity
        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicketId(ticketId);
        attachment.setReplyId(null);
        attachment.setFilename(storageKey);
        attachment.setOriginalFilename(originalFilename);
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setStoragePath(storageKey);
        ticketAttachmentMapper.insert(attachment);

        log.info("Attachment uploaded: ticketId={} storageKey={} size={} by userId={}",
                ticketId, storageKey, file.getSize(), userId);
        return TicketAttachmentResponse.from(attachment);
    }

    // ──────────────────────────────────────────────
    //  Download Attachment
    // ──────────────────────────────────────────────

    @Override
    public Resource downloadAttachment(Long attachmentId) {
        TicketAttachment attachment = ticketAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(ErrorCode.TICKET_ATTACHMENT_NOT_FOUND);
        }

        try {
            return fileStorage.load(attachment.getStoragePath());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TICKET_ATTACHMENT_NOT_FOUND);
        }
    }

    // ──────────────────────────────────────────────
    //  Batch Delete
    // ──────────────────────────────────────────────

    @Override
    @Transactional
    public int deleteBatchTickets(List<Long> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) return 0;

        // Clean up attachment files before deleting records
        LambdaQueryWrapper<TicketAttachment> attachWrapper = new LambdaQueryWrapper<TicketAttachment>()
                .in(TicketAttachment::getTicketId, ticketIds);
        List<TicketAttachment> attachments = ticketAttachmentMapper.selectList(attachWrapper);
        for (TicketAttachment att : attachments) {
            fileStorage.delete(att.getStoragePath());
        }

        int deleted = ticketMapper.deleteBatchIds(ticketIds);
        log.info("Batch deleted {} tickets: ids={}", deleted, ticketIds);
        return deleted;
    }

    // ──────────────────────────────────────────────
    //  Dashboard Stats
    // ──────────────────────────────────────────────

    @Override
    public DashboardStatsResponse getDashboardStats(Long userId, String role) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        // Regular users see only their own tickets
        if (RoleConstants.ROLE_USER.equals(role)) {
            wrapper.eq(Ticket::getCreatedBy, userId);
        }

        List<Ticket> tickets = ticketMapper.selectList(wrapper);
        long open = 0, inProgress = 0, resolved = 0, closed = 0;
        for (Ticket t : tickets) {
            String s = t.getStatus();
            if (BusinessConstants.TICKET_STATUS_OPEN.equals(s)) open++;
            else if (BusinessConstants.TICKET_STATUS_IN_PROGRESS.equals(s)) inProgress++;
            else if (BusinessConstants.TICKET_STATUS_RESOLVED.equals(s)) resolved++;
            else if (BusinessConstants.TICKET_STATUS_CLOSED.equals(s)) closed++;
        }
        return new DashboardStatsResponse(tickets.size(), open, inProgress, resolved, closed);
    }

    // ──────────────────────────────────────────────
    //  Private Helpers
    // ──────────────────────────────────────────────

    private Ticket findTicketOrFail(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new TicketNotFoundException();
        }
        return ticket;
    }

    private void checkTicketAccess(Ticket ticket, Long userId, String role) {
        // Agents and admins can access all tickets
        if (RoleConstants.ROLE_AGENT.equals(role) || RoleConstants.ROLE_ADMIN.equals(role)) {
            return;
        }
        // Regular users can only access their own tickets
        if (!ticket.getCreatedBy().equals(userId)) {
            throw new TicketAccessDeniedException();
        }
    }

    private void validatePriority(String priority) {
        if (!VALID_PRIORITIES.contains(priority)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid priority: " + priority);
        }
    }

    private void validateCategory(String category) {
        if (!VALID_CATEGORIES.contains(category)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid category: " + category);
        }
    }

    private String getUsername(Long userId) {
        if (userId == null || userId == 0L) return null;
        User user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : "Unknown";
    }

    private Map<Long, String> getUsernameMap(Set<Long> userIds) {
        // Filter out null and sentinel values
        Set<Long> validIds = userIds.stream()
                .filter(id -> id != null && id != 0L)
                .collect(Collectors.toSet());
        if (validIds.isEmpty()) return Collections.emptyMap();
        // Batch query — single DB round-trip instead of N+1
        return userMapper.selectBatchIds(validIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }
}