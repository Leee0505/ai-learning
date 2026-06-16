package com.ticket.dto.response;

public class AgentStatsResponse {

    /** Tickets completed today (resolved + closed) */
    private long todayDone;
    /** Average processing time in minutes (created → resolved) for tickets completed today */
    private double avgProcessingMinutes;
    /** Unassigned tickets in pending queue */
    private long pendingCount;
    /** Active tickets currently assigned to this agent */
    private long activeCount;
    /** Daily completion counts for the last 7 days — labels + values aligned by index */
    private String[] chartLabels;
    private long[] chartValues;

    public AgentStatsResponse() {}

    public AgentStatsResponse(long todayDone, double avgProcessingMinutes, long pendingCount,
                              long activeCount, String[] chartLabels, long[] chartValues) {
        this.todayDone = todayDone;
        this.avgProcessingMinutes = avgProcessingMinutes;
        this.pendingCount = pendingCount;
        this.activeCount = activeCount;
        this.chartLabels = chartLabels;
        this.chartValues = chartValues;
    }

    public long getTodayDone() { return todayDone; }
    public double getAvgProcessingMinutes() { return avgProcessingMinutes; }
    public long getPendingCount() { return pendingCount; }
    public long getActiveCount() { return activeCount; }
    public String[] getChartLabels() { return chartLabels; }
    public long[] getChartValues() { return chartValues; }
}
