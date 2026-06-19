package com.ticket.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateSlaRequest {

    @NotNull(message = "responseMinutes is required")
    @Min(value = 1, message = "responseMinutes must be at least 1")
    @Max(value = 43200, message = "responseMinutes must not exceed 43200 (30 days)")
    private Integer responseMinutes;

    @NotNull(message = "resolutionMinutes is required")
    @Min(value = 1, message = "resolutionMinutes must be at least 1")
    @Max(value = 86400, message = "resolutionMinutes must not exceed 86400 (60 days)")
    private Integer resolutionMinutes;
}
