package com.ticket.controller;

import com.ticket.common.constant.RoleConstants;
import com.ticket.dto.response.*;
import com.ticket.service.MonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/monitor")
@PreAuthorize("hasRole('" + RoleConstants.ADMIN + "')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Monitor", description = "Admin system monitoring endpoints")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get health overview for all monitored services")
    public ApiResult<MonitorOverviewResponse> getOverview() {
        return ApiResult.success(monitorService.getOverview());
    }

    @GetMapping("/kafka")
    @Operation(summary = "Get Kafka consumer group metrics")
    public ApiResult<KafkaMetricsResponse> getKafka() {
        return ApiResult.success(monitorService.getKafkaMetrics());
    }

    @GetMapping("/redis")
    @Operation(summary = "Get Redis cache metrics")
    public ApiResult<RedisMetricsResponse> getRedis() {
        return ApiResult.success(monitorService.getRedisMetrics());
    }

    @GetMapping("/api")
    @Operation(summary = "Get API latency percentiles")
    public ApiResult<ApiMetricsResponse> getApi() {
        return ApiResult.success(monitorService.getApiMetrics());
    }
}
