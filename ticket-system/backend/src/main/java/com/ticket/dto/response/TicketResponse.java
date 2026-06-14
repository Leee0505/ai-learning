package com.ticket.dto.response;

import com.ticket.entity.Ticket;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ticket summary — used in list views")
public class TicketResponse {

    @Schema(description = "Ticket ID", example = "1")
    private Long id;

    @Schema(description = "Ticket title", example = "Unable to reset password")
    private String title;

    @Schema(description = "Current status", example = "OPEN", allowableValues = {"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"})
    private String status;

    @Schema(description = "Priority level", example = "MEDIUM", allowableValues = {"LOW", "MEDIUM", "HIGH", "URGENT"})
    private String priority;

    @Schema(description = "Category", example = "ACCOUNT_ISSUE")
    private String category;

    @Schema(description = "ID of the assigned agent (null if unassigned)", example = "5", nullable = true)
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

    public void setAssignedToName(String name) { this.assignedToName = name; }
    public void setCreatedByName(String name) { this.createdByName = name; }
}
