package com.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KafkaMetricsResponse {
    private boolean connected;
    private int activeConsumers;
    private int totalPartitions;
}
