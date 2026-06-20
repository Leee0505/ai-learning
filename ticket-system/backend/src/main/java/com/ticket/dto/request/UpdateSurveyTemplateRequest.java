package com.ticket.dto.request;

import lombok.Data;

@Data
public class UpdateSurveyTemplateRequest {
    private String title;
    private String description;
    private String status;
}
