package com.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for changing a ticket's status")
public class ChangeStatusRequest {

    @NotBlank(message = "status is required")
    @Schema(description = "Target status. Valid transitions: OPEN→IN_PROGRESS|CLOSED, IN_PROGRESS→RESOLVED|CLOSED, RESOLVED→CLOSED",
            example = "IN_PROGRESS", allowableValues = {"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"})
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
