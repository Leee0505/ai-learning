package com.ticket.dto.response;

import com.ticket.entity.TicketAttachment;

public class TicketAttachmentResponse {
    private Long id;
    private Long ticketId;
    private Long replyId;
    private String filename;
    private String originalFilename;
    private Long fileSize;
    private String contentType;
    private Long createdDate;

    public static TicketAttachmentResponse from(TicketAttachment attachment) {
        TicketAttachmentResponse r = new TicketAttachmentResponse();
        r.id = attachment.getId();
        r.ticketId = attachment.getTicketId();
        r.replyId = attachment.getReplyId();
        r.filename = attachment.getFilename();
        r.originalFilename = attachment.getOriginalFilename();
        r.fileSize = attachment.getFileSize();
        r.contentType = attachment.getContentType();
        r.createdDate = attachment.getCreatedDate();
        return r;
    }

    public Long getId() { return id; }
    public Long getTicketId() { return ticketId; }
    public Long getReplyId() { return replyId; }
    public String getFilename() { return filename; }
    public String getOriginalFilename() { return originalFilename; }
    public Long getFileSize() { return fileSize; }
    public String getContentType() { return contentType; }
    public Long getCreatedDate() { return createdDate; }
}