package com.ticket.service;

import com.ticket.dto.request.*;
import com.ticket.dto.response.*;

import com.ticket.dto.response.PageResponse;

public interface SurveyService {
    // Template CRUD
    SurveyTemplateResponse createTemplate(CreateSurveyTemplateRequest request, Long adminId);
    SurveyTemplateResponse getTemplate(Long templateId);
    PageResponse<SurveyTemplateResponse> listTemplates(int page, int size);
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

    // Instance management
    SurveyInstanceResponse createInstance(CreateSurveyInstanceRequest request, Long adminId);
    List<SurveyInstanceResponse> listUserInstances(Long userId);
    List<SurveyInstanceResponse> listTemplateInstances(Long templateId);

    // Fill flow
    SurveyFillResponse getFillData(Long instanceId, Long userId);
    void saveAnswer(Long instanceId, SaveAnswerRequest request, Long userId);
    SurveyInstanceResponse submitSurvey(Long instanceId, SubmitSurveyRequest request, Long userId);
}
