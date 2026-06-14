package com.ticket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dashboard statistics — ticket counts by status for the current user (or all tickets for agent/admin)")
public class DashboardStatsResponse {

    @Schema(description = "Total number of tickets visible to the current user", example = "15")
    private long total;

    @Schema(description = "Tickets with status OPEN", example = "3")
    private long open;

    @Schema(description = "Tickets with status IN_PROGRESS", example = "4")
    private long inProgress;

    @Schema(description = "Tickets with status RESOLVED", example = "6")
    private long resolved;

    @Schema(description = "Tickets with status CLOSED", example = "2")
    private long closed;

    public DashboardStatsResponse(long total, long open, long inProgress, long resolved, long closed) {
        this.total = total;
        this.open = open;
        this.inProgress = inProgress;
        this.resolved = resolved;
        this.closed = closed;
    }

    public long getTotal() { return total; }
    public long getOpen() { return open; }
    public long getInProgress() { return inProgress; }
    public long getResolved() { return resolved; }
    public long getClosed() { return closed; }
}
