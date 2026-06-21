package com.ticket.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class SurveyTemplateResponse {
    private Long id;
    private Long tenantId;
    private String title;
    private String description;
    private String status;
    private Integer version;
    private Long originId;
    private Boolean allowResubmit;
    private Long createdDate;
    private List<PageResponse> pages;

    @Data
    public static class PageResponse {
        private Long id;
        private String title;
        private Integer displayOrder;
        private List<SectionResponse> sections;
        private List<VisibilityRuleResponse> visibilityRules;
    }

    @Data
    public static class SectionResponse {
        private Long id;
        private String title;
        private String description;
        private Integer displayOrder;
        private List<QuestionResponse> questions;
        private List<VisibilityRuleResponse> visibilityRules;
    }

    @Data
    public static class QuestionResponse {
        private Long id;
        private String type;
        private String title;
        private String description;
        private String options;
        private Boolean required;
        private Integer displayOrder;
        private List<VisibilityRuleResponse> visibilityRules;
    }

    @Data
    public static class VisibilityRuleResponse {
        private Long id;
        private Long sourceQuestionId;
        private String op;
        private String value;
        private String ruleType;
    }
}
