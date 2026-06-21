package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddVisibilityRuleRequest {
    @NotBlank
    private String targetType;
    @NotNull
    private Long targetId;
    @NotNull
    private Long sourceQuestionId;
    @NotBlank
    private String op;
    private String value;
    private String ruleType = "AND";
}
