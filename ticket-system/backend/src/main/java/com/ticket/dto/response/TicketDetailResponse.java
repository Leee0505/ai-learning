package com.ticket.dto.response;

import com.ticket.entity.Ticket;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Full ticket detail — includes reply timeline, attachment list, and display names")
public class TicketDetailResponse {

    @Schema(description = "Ticket ID", example = "1")
    private Long id;

    @Schema(description = "Ticket title", example = "Unable to reset password")
    private String title;

    @Schema(description = "Detailed description of the issue", example = "When I click 'Forgot Password'...")
    private String description;

    @Schema(description = "Current status", example = "IN_PROGRESS")
    private String status;

    @Schema(description = "Priority level", example = "HIGH")
    private String priority;

    @Schema(description = "Category", example = "ACCOUNT_ISSUE")
    private String category;

    @Schema(description = "ID of the assigned agent", example = "5", nullable = true)
    private Long assignedTo;

    @Schema(description = "Display name of the assigned agent", example = "agent1", nullable = true)
    private String assignedToName;

    @Schema(description = "Display name of the ticket creator", example = "user1")
    private String createdByName;

    @Schema(description = "Unix timestamp (ms) when ticket was resolved", example = "1700123400000", nullable = true)
    private Long resolvedDate;

    @Schema(description = "Unix timestamp (ms) when ticket was closed", example = "1700123400000", nullable = true)
    private Long closedDate;

    @Schema(description = "Unix timestamp (ms) when ticket was created", example = "1700000000000")
    private Long createdDate;

    @Schema(description = "Reply timeline — filtered by role (internal notes hidden from regular users)")
    private List<TicketReplyResponse> replies;

    @Schema(description = "List of attached files")
    private List<TicketAttachmentResponse> attachments;

    public static TicketDetailResponse from(Ticket ticket) {
        TicketDetailResponse r = new TicketDetailResponse();
        r.id = ticket.getId();
        r.title = ticket.getTitle();
        r.description = ticket.getDescription();
        r.status = ticket.getStatus();
        r.priority = ticket.getPriority();
        r.category = ticket.getCategory();
        r.assignedTo = ticket.getAssignedTo();
        r.resolvedDate = ticket.getResolvedDate();
        r.closedDate = ticket.getClosedDate();
        r.createdDate = ticket.getCreatedDate();
        return r;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public String getCategory() { return category; }
    public Long getAssignedTo() { return assignedTo; }
    public String getAssignedToName() { return assignedToName; }
    public String getCreatedByName() { return createdByName; }
    public Long getResolvedDate() { return resolvedDate; }
    public Long getClosedDate() { return closedDate; }
    public Long getCreatedDate() { return createdDate; }
    public List<TicketReplyResponse> getReplies() { return replies; }
    public List<TicketAttachmentResponse> getAttachments() { return attachments; }

    public void setAssignedToName(String name) { this.assignedToName = name; }
    public void setCreatedByName(String name) { this.createdByName = name; }
    public void setReplies(List<TicketReplyResponse> replies) { this.replies = replies; }
    public void setAttachments(List<TicketAttachmentResponse> attachments) { this.attachments = attachments; }
}
