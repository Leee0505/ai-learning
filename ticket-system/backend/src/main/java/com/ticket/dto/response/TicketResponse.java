package com.ticket.dto.response;

import com.ticket.entity.Ticket;

public class TicketResponse {
    private Long id;
    private String title;
    private String status;
    private String priority;
    private String category;
    private Long assignedTo;
    private String assignedToName;
    private String createdByName;
    private Long resolvedDate;
    private Long closedDate;
    private Long createdDate;

    public static TicketResponse from(Ticket ticket) {
        TicketResponse r = new TicketResponse();
        r.id = ticket.getId();
        r.title = ticket.getTitle();
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
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public String getCategory() { return category; }
    public Long getAssignedTo() { return assignedTo; }
    public String getAssignedToName() { return assignedToName; }
    public String getCreatedByName() { return createdByName; }
    public Long getResolvedDate() { return resolvedDate; }
    public Long getClosedDate() { return closedDate; }
    public Long getCreatedDate() { return createdDate; }

    // Setters for joined fields (set after from())
    public void setAssignedToName(String name) { this.assignedToName = name; }
    public void setCreatedByName(String name) { this.createdByName = name; }
}