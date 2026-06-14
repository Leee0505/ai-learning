package com.ticket.dto.request;

import jakarta.validation.constraints.NotNull;

public class AssignTicketRequest {
    @NotNull(message = "assignedTo is required")
    private Long assignedTo;

    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
}