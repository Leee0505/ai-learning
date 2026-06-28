package com.ticket.service;

import com.ticket.dto.response.TicketAttachmentResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface TicketAttachmentService {
    TicketAttachmentResponse uploadAttachment(Long ticketId, MultipartFile file, Long userId, String role);
    Resource downloadAttachment(Long attachmentId, Long userId, String role);
}
