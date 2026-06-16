package com.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request body for enabling or disabling a user")
public class UserStatusUpdateRequest {

    @NotNull(message = "status is required")
    @Pattern(regexp = "[01]", message = "status must be 0 (disabled) or 1 (enabled)")
    @Schema(description = "New status: 1 = enabled, 0 = disabled", example = "1")
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
