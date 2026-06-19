package com.ticket.dto.response;

import lombok.Data;

@Data
public class SlaConfigResponse {
    private Long id;
    private String priority;
    private Integer responseMinutes;
    private Integer resolutionMinutes;
    private Boolean active;
}
