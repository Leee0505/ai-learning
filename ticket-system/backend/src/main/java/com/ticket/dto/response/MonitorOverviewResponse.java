package com.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonitorOverviewResponse {
    private String kafkaStatus;
    private String redisStatus;
    private String apiStatus;
}
