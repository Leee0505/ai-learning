package com.ticket.service;

import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface TicketService {
    TicketDetailResponse createTicket(CreateTicketRequest request, Long userId);
    PageResponse<TicketResponse> listTickets(String status, String priority, String category,
                                              String keyword, int page, int size, Long userId, String role);
    TicketDetailResponse getTicketDetail(Long ticketId, Long userId, String role);
    TicketDetailResponse updateTicket(Long ticketId, UpdateTicketRequest request, Long userId, String role);
    void deleteTicket(Long ticketId);
    TicketDetailResponse changeStatus(Long ticketId, ChangeStatusRequest request, Long userId, String role);
    TicketDetailResponse assignTicket(Long ticketId, AssignTicketRequest request, Long userId, String role);
    TicketReplyResponse addReply(Long ticketId, CreateReplyRequest request, Long userId);
    TicketAttachmentResponse uploadAttachment(Long ticketId, MultipartFile file, Long userId);
    Resource downloadAttachment(Long attachmentId);
}