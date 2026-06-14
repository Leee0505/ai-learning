package com.ticket.dto.response;

import com.ticket.entity.Ticket;

import java.util.List;

public class TicketDetailResponse {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String category;
    private Long assignedTo;
    private String assignedToName;
    private String createdByName;
    private Long resolvedDate;
    private Long closedDate;
    private Long createdDate;
    private List<TicketReplyResponse> replies;
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

    // Setters for joined/loaded fields
    public void setAssignedToName(String name) { this.assignedToName = name; }
    public void setCreatedByName(String name) { this.createdByName = name; }
    public void setReplies(List<TicketReplyResponse> replies) { this.replies = replies; }
    public void setAttachments(List<TicketAttachmentResponse> attachments) { this.attachments = attachments; }
}