package com.ticket.event;

import java.time.Instant;

public class TicketOverdueEvent {
    private Long ticketId;
    private String type; // UNASSIGNED_OVERDUE | RESOLUTION_OVERDUE
    private int overdueMinutes;
    private Long assignedTo;
    private String priority;
    private String timestamp;

    public TicketOverdueEvent() {}

    public TicketOverdueEvent(Long ticketId, String type, int overdueMinutes, Long assignedTo, String priority) {
        this.ticketId = ticketId;
        this.type = type;
        this.overdueMinutes = overdueMinutes;
        this.assignedTo = assignedTo;
        this.priority = priority;
        this.timestamp = Instant.now().toString();
    }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getOverdueMinutes() { return overdueMinutes; }
    public void setOverdueMinutes(int overdueMinutes) { this.overdueMinutes = overdueMinutes; }
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
