package com.ticket.service;

import com.ticket.dto.request.*;
import com.ticket.dto.response.*;

import java.util.List;

public interface SurveyService {
    // Template CRUD
    SurveyTemplateResponse createTemplate(CreateSurveyTemplateRequest request, Long adminId);
    SurveyTemplateResponse getTemplate(Long templateId);
    List<SurveyTemplateResponse> listTemplates();
    SurveyTemplateResponse updateTemplate(Long id, UpdateSurveyTemplateRequest request, Long adminId);
    void deleteTemplate(Long id);

    // Builder — pages
    SurveyTemplateResponse.PageResponse addPage(Long templateId, String title, Long adminId);
    void deletePage(Long pageId);
    void reorderPages(Long templateId, ReorderRequest request);

    // Builder — sections
    SurveyTemplateResponse.SectionResponse addSection(Long pageId, String title, Long adminId);
    void deleteSection(Long sectionId);

    // Builder — questions
    SurveyTemplateResponse.QuestionResponse addQuestion(Long sectionId, AddQuestionRequest request, Long adminId);
    void updateQuestion(Long questionId, AddQuestionRequest request, Long adminId);
    void deleteQuestion(Long questionId);
    void reorderQuestions(Long sectionId, ReorderRequest request);

    // Builder — visibility rules
    SurveyTemplateResponse.VisibilityRuleResponse addVisibilityRule(Long templateId, AddVisibilityRuleRequest request, Long adminId);
    void deleteVisibilityRule(Long ruleId);
}
