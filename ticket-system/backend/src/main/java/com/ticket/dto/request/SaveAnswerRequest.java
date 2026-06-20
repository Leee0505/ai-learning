package com.ticket.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveAnswerRequest {
    @NotNull private Long questionId;
    private String value; // JSON string for complex types, plain string for simple types
}
