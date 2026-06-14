package com.ticket.dto.response;

import com.ticket.entity.TicketAttachment;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata for an uploaded file attachment")
public class TicketAttachmentResponse {

    @Schema(description = "Attachment ID", example = "20")
    private Long id;

    @Schema(description = "Parent ticket ID", example = "1", nullable = true)
    private Long ticketId;

    @Schema(description = "Parent reply ID (null if ticket-level attachment)", example = "10", nullable = true)
    private Long replyId;

    @Schema(description = "Storage filename on disk (UUID + extension)", example = "a1b2c3d4-...pdf")
    private String filename;

    @Schema(description = "Original filename as uploaded by the user", example = "screenshot.png")
    private String originalFilename;

    @Schema(description = "File size in bytes", example = "1048576")
    private Long fileSize;

    @Schema(description = "MIME type of the file", example = "image/png")
    private String contentType;

    @Schema(description = "Unix timestamp (ms) when attachment was uploaded", example = "1700000200000")
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
