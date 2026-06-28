package com.ticket.service.impl;

import com.ticket.common.constant.BusinessConstants;
import com.ticket.common.constant.ErrorCode;
import com.ticket.common.constant.RoleConstants;
import com.ticket.common.exception.BusinessException;
import com.ticket.dto.response.TicketAttachmentResponse;
import com.ticket.entity.Ticket;
import com.ticket.entity.TicketAttachment;
import com.ticket.mapper.TicketAttachmentMapper;
import com.ticket.mapper.TicketMapper;
import com.ticket.service.TicketAttachmentService;
import com.ticket.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
public class TicketAttachmentServiceImpl implements TicketAttachmentService {

    private static final Logger log = LoggerFactory.getLogger(TicketAttachmentServiceImpl.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "zip", "rar", "7z",
            "mp3", "wav", "mp4", "mov", "webm");

    private final TicketAttachmentMapper attachmentMapper;
    private final TicketMapper ticketMapper;
    private final FileStorageService fileStorage;

    public TicketAttachmentServiceImpl(TicketAttachmentMapper attachmentMapper,
                                        TicketMapper ticketMapper,
                                        FileStorageService fileStorage) {
        this.attachmentMapper = attachmentMapper;
        this.ticketMapper = ticketMapper;
        this.fileStorage = fileStorage;
    }

    @Override
    @Transactional
    public TicketAttachmentResponse uploadAttachment(Long ticketId, MultipartFile file, Long userId, String role) {
        Ticket ticket = findTicketOrFail(ticketId);
        checkTicketAccess(ticket, userId, role);

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
        attachmentMapper.insert(attachment);

        log.info("Attachment uploaded: ticketId={} storageKey={} size={} by userId={}",
                ticketId, storageKey, file.getSize(), userId);
        return TicketAttachmentResponse.from(attachment);
    }

    @Override
    public Resource downloadAttachment(Long attachmentId, Long userId, String role) {
        TicketAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(ErrorCode.TICKET_ATTACHMENT_NOT_FOUND);
        }

        // Verify access to the parent ticket
        Ticket ticket = findTicketOrFail(attachment.getTicketId());
        checkTicketAccess(ticket, userId, role);

        try {
            return fileStorage.load(attachment.getStoragePath());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TICKET_ATTACHMENT_NOT_FOUND);
        }
    }

    // ── Helpers ──

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
}
