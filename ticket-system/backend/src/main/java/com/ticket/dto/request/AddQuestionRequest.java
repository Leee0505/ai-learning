package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddQuestionRequest {
    @NotBlank
    private String type;
    @NotBlank
    private String title;
    private String description;
    private String options;
    private Integer required = 0;
}
