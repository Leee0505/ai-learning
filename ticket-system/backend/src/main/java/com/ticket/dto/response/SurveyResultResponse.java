package com.ticket.dto.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SurveyResultResponse {
    private Long templateId;
    private String templateTitle;
    private int totalInstances;
    private int completedInstances;
    private List<QuestionResult> questions;

    @Data
    public static class QuestionResult {
        private Long questionId;
        private String title;
        private String type;
        private Map<String, Integer> choiceCounts; // option → count (for choice types)
        private List<String> textAnswers;           // raw answers (for text types)
    }
}
