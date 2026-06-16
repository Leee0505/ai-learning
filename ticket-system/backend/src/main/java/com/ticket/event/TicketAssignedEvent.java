package com.ticket.event;

import java.time.Instant;

/**
 * Published to topic "ticket.assigned" when an agent claims or is assigned a ticket.
 */
public class TicketAssignedEvent {

    private Long ticketId;
    private Long assignedTo;
    private Long assignedBy;
    private String timestamp;

    public TicketAssignedEvent() {}

    public TicketAssignedEvent(Long ticketId, Long assignedTo, Long assignedBy) {
        this.ticketId = ticketId;
        this.assignedTo = assignedTo;
        this.assignedBy = assignedBy;
        this.timestamp = Instant.now().toString();
    }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public Long getAssignedBy() { return assignedBy; }
    public void setAssignedBy(Long assignedBy) { this.assignedBy = assignedBy; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
