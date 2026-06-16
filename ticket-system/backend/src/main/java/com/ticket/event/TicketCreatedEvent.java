package com.ticket.event;

import java.time.Instant;

/**
 * Published to topic "ticket.created" when a new ticket is submitted.
 */
public class TicketCreatedEvent {

    private Long ticketId;
    private String title;
    private String priority;
    private String category;
    private Long createdBy;
    private String timestamp;

    public TicketCreatedEvent() {}

    public TicketCreatedEvent(Long ticketId, String title, String priority,
                              String category, Long createdBy) {
        this.ticketId = ticketId;
        this.title = title;
        this.priority = priority;
        this.category = category;
        this.createdBy = createdBy;
        this.timestamp = Instant.now().toString();
    }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
