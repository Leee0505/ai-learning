package com.ticket.dto.response;

import lombok.Data;

@Data
public class SurveyInstanceResponse {
    private Long id;
    private Long tenantId;
    private String title;
    private String status;
    private Long assignedTo;
    private String triggerType;
    private Long ticketId;
    private String templateTitle;
    private Integer totalPages;
    private Integer completedPages;
    private Long createdDate;
}
