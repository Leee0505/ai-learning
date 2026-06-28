package com.ticket.service.impl;

import com.ticket.common.constant.BusinessConstants;
import com.ticket.common.constant.ErrorCode;
import com.ticket.common.constant.RoleConstants;
import com.ticket.common.exception.BusinessException;
import com.ticket.dto.request.CreateReplyRequest;
import com.ticket.dto.request.UpdateReplyRequest;
import com.ticket.dto.response.TicketReplyResponse;
import com.ticket.entity.Ticket;
import com.ticket.entity.TicketReply;
import com.ticket.mapper.TicketMapper;
import com.ticket.mapper.TicketReplyMapper;
import com.ticket.mapper.UserMapper;
import com.ticket.service.TicketReplyService;
import com.ticket.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketReplyServiceImpl implements TicketReplyService {

    private static final Logger log = LoggerFactory.getLogger(TicketReplyServiceImpl.class);

    private final TicketReplyMapper ticketReplyMapper;
    private final TicketMapper ticketMapper;
    private final UserMapper userMapper;

    public TicketReplyServiceImpl(TicketReplyMapper ticketReplyMapper,
                                   TicketMapper ticketMapper,
                                   UserMapper userMapper) {
        this.ticketReplyMapper = ticketReplyMapper;
        this.ticketMapper = ticketMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public TicketReplyResponse addReply(Long ticketId, CreateReplyRequest request, Long userId, String role) {
        Ticket ticket = findTicketOrFail(ticketId);
        checkTicketAccess(ticket, userId, role);

        TicketReply reply = new TicketReply();
        reply.setTenantId(SecurityUtils.getCurrentTenantId());
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

    @Override
    @Transactional
    public TicketReplyResponse editReply(Long ticketId, Long replyId, UpdateReplyRequest request, Long userId) {
        TicketReply reply = ticketReplyMapper.selectById(replyId);
        if (reply == null || !reply.getTicketId().equals(ticketId)) {
            throw new BusinessException(ErrorCode.REPLY_NOT_FOUND);
        }
        if (!reply.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.REPLY_EDIT_PERMISSION_DENIED);
        }

        reply.setContent(request.getContent());
        reply.setIsEdited(1);
        ticketReplyMapper.updateById(reply);

        TicketReplyResponse response = TicketReplyResponse.from(reply);
        response.setUsername(getUsername(userId));

        log.info("Reply edited: ticketId={} replyId={} by userId={}", ticketId, replyId, userId);
        return response;
    }

    @Override
    @Transactional
    public void deleteReply(Long ticketId, Long replyId, Long userId, String role) {
        TicketReply reply = ticketReplyMapper.selectById(replyId);
        if (reply == null || !reply.getTicketId().equals(ticketId)) {
            throw new BusinessException(ErrorCode.REPLY_NOT_FOUND);
        }
        if (!reply.getUserId().equals(userId) && !RoleConstants.ROLE_ADMIN.equals(role)) {
            throw new BusinessException(ErrorCode.REPLY_DELETE_PERMISSION_DENIED);
        }

        ticketReplyMapper.deleteById(replyId);
        log.info("Reply deleted: ticketId={} replyId={} by userId={}", ticketId, replyId, userId);
    }

    // ── Helpers (shared with TicketServiceImpl) ──

    private Ticket findTicketOrFail(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.TICKET_NOT_FOUND);
        }
        return ticket;
    }

    private void checkTicketAccess(Ticket ticket, Long userId, String role) {
        if (RoleConstants.ROLE_ADMIN.equals(role)) return;
        if (RoleConstants.ROLE_AGENT.equals(role)) return;
        if (!ticket.getCreatedBy().equals(userId)) {
            throw new BusinessException(ErrorCode.TICKET_ACCESS_DENIED);
        }
    }

    private String getUsername(Long userId) {
        var user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : "Unknown";
    }
}
