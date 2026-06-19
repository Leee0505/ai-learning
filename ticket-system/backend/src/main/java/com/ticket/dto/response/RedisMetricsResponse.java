package com.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RedisMetricsResponse {
    private boolean connected;
    private double hitRate;
    private long totalKeys;
    private long usedMemoryBytes;
}
