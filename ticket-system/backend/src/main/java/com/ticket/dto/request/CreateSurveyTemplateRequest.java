package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSurveyTemplateRequest {
    @NotBlank(message = "title is required")
    @Size(max = 255)
    private String title;

    private String description;
    private Integer allowResubmit = 0;
}
