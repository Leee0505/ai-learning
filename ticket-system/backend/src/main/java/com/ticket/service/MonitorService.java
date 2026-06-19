package com.ticket.service;

import com.ticket.dto.response.*;

public interface MonitorService {
    MonitorOverviewResponse getOverview();
    KafkaMetricsResponse getKafkaMetrics();
    RedisMetricsResponse getRedisMetrics();
    ApiMetricsResponse getApiMetrics();
}
