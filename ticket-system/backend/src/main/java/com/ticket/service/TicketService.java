package com.ticket.service;

import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TicketService {
    TicketDetailResponse createTicket(CreateTicketRequest request, Long userId);
    PageResponse<TicketResponse> listTickets(String status, String priority, String category,
                                              String keyword, String assignedTo,
                                              int page, int size,
                                              Long userId, String role, String sortOrder);
    TicketDetailResponse getTicketDetail(Long ticketId, Long userId, String role);
    TicketDetailResponse updateTicket(Long ticketId, UpdateTicketRequest request, Long userId, String role);
    void deleteTicket(Long ticketId);
    int deleteBatchTickets(List<Long> ticketIds);
    TicketDetailResponse changeStatus(Long ticketId, ChangeStatusRequest request, Long userId, String role);
    TicketDetailResponse assignTicket(Long ticketId, AssignTicketRequest request, Long userId, String role);
    TicketReplyResponse addReply(Long ticketId, CreateReplyRequest request, Long userId);
    TicketReplyResponse editReply(Long ticketId, Long replyId, UpdateReplyRequest request, Long userId);
    void deleteReply(Long ticketId, Long replyId, Long userId, String role);
    TicketAttachmentResponse uploadAttachment(Long ticketId, MultipartFile file, Long userId);
    Resource downloadAttachment(Long attachmentId);

    DashboardStatsResponse getDashboardStats(Long userId, String role);
    AgentStatsResponse getAgentStats(Long userId);

    Resource exportTickets(String format, String status, String priority, String category,
                           String keyword, String assignedTo, Long userId, String role);
}