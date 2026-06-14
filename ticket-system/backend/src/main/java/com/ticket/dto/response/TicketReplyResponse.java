package com.ticket.dto.response;

import com.ticket.entity.TicketReply;

public class TicketReplyResponse {
    private Long id;
    private Long ticketId;
    private Long userId;
    private String username;
    private String content;
    private boolean isInternal;
    private Long createdDate;

    public static TicketReplyResponse from(TicketReply reply) {
        TicketReplyResponse r = new TicketReplyResponse();
        r.id = reply.getId();
        r.ticketId = reply.getTicketId();
        r.userId = reply.getUserId();
        r.content = reply.getContent();
        r.isInternal = reply.getIsInternal() != null && reply.getIsInternal() == 1;
        r.createdDate = reply.getCreatedDate();
        return r;
    }

    public Long getId() { return id; }
    public Long getTicketId() { return ticketId; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getContent() { return content; }
    public boolean getIsInternal() { return isInternal; }
    public Long getCreatedDate() { return createdDate; }

    // Setter for joined username
    public void setUsername(String username) { this.username = username; }
}