package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ChangeStatusRequest {
    @NotBlank(message = "status is required")
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}