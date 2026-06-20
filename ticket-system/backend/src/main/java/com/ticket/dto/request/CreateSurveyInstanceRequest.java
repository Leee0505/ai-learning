package com.ticket.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSurveyInstanceRequest {
    @NotNull private Long templateId;
    @NotNull private Long assignedTo;
    private String triggerType = "MANUAL";
    private Long ticketId;
}
