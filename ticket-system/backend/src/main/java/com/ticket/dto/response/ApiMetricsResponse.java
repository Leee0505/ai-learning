package com.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiMetricsResponse {
    private long requestCount;
    private long errorCount;
    private long p50Ms;
    private long p95Ms;
    private long p99Ms;
}
