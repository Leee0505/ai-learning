package com.ticket.dto.response;

import com.ticket.entity.TicketReply;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A reply within a ticket's activity timeline")
public class TicketReplyResponse {

    @Schema(description = "Reply ID", example = "10")
    private Long id;

    @Schema(description = "Parent ticket ID", example = "1")
    private Long ticketId;

    @Schema(description = "Author user ID", example = "1")
    private Long userId;

    @Schema(description = "Display name of the reply author", example = "user1")
    private String username;

    @Schema(description = "Reply content (plain text)", example = "Thank you for reporting this issue.")
    private String content;

    @Schema(description = "Whether this is an internal note (agent/admin only). Hidden from regular users in the detail view.", example = "false")
    private boolean isInternal;

    @Schema(description = "Unix timestamp (ms) when reply was created", example = "1700000100000")
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

    public void setUsername(String username) { this.username = username; }
}
