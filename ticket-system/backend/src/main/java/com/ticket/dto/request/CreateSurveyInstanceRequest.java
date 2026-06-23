package com.ticket.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CreateSurveyInstanceRequest {
    @NotNull private Long templateId;
    @NotNull private Long assignedTo;
    private String triggerType = "MANUAL";
    private Long ticketId;
    /** Per-page assignee overrides (pageId → userId). Pages not in this map inherit from assignedTo. */
    private Map<Long, Long> pageAssignees;
}
