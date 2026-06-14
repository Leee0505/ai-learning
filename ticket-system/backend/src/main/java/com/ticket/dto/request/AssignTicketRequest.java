package com.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for assigning a ticket to an agent")
public class AssignTicketRequest {

    @NotNull(message = "assignedTo is required")
    @Schema(description = "ID of the agent user to assign this ticket to. The target must have ROLE_AGENT.", example = "5")
    private Long assignedTo;

    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
}
