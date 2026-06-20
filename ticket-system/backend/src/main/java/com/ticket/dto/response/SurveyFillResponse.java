package com.ticket.dto.response;

import lombok.Data;
import java.util.*;

@Data
public class SurveyFillResponse {
    private Long instanceId;
    private String instanceStatus;
    private String title;
    private List<SurveyTemplateResponse.PageResponse> pages;
    private Set<String> hiddenTargets; // "PAGE:123", "SECTION:456", "QUESTION:789"
    private Map<Long, String> existingAnswers; // questionId → value JSON
}
