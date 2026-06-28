package com.ticket.service;

import com.ticket.dto.request.CreateReplyRequest;
import com.ticket.dto.request.UpdateReplyRequest;
import com.ticket.dto.response.TicketReplyResponse;

public interface TicketReplyService {
    TicketReplyResponse addReply(Long ticketId, CreateReplyRequest request, Long userId, String role);
    TicketReplyResponse editReply(Long ticketId, Long replyId, UpdateReplyRequest request, Long userId);
    void deleteReply(Long ticketId, Long replyId, Long userId, String role);
}
