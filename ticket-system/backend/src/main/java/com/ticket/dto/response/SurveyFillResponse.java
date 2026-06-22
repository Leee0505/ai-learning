package com.ticket.dto.response;

import lombok.Data;
import java.util.*;

@Data
public class SurveyFillResponse {
    private Long instanceId;
    private String instanceStatus;
    private String assignedToName;
    private String currentUsername;
    private String title;
    private List<SurveyTemplateResponse.PageResponse> pages;
    private Set<String> hiddenTargets; // "PAGE:123", "SECTION:456", "QUESTION:789"
    private Map<Long, String> existingAnswers; // questionId → value JSON
    private Map<Long, String> pageStatuses; // pageId → status (READY/IN_PROGRESS/SUBMITTED)
    private Map<Long, String> pageAssignees; // pageId → username
}
