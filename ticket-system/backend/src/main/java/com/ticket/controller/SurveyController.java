package com.ticket.controller;

import com.ticket.common.constant.RoleConstants;
import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.SurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/surveys")
@PreAuthorize("hasRole('" + RoleConstants.ADMIN + "')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Survey Admin", description = "Admin survey template management")
public class SurveyController {

    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    // ── Template CRUD ──

    @GetMapping
    @Operation(summary = "List all survey templates")
    public ApiResult<List<SurveyTemplateResponse>> list() {
        return ApiResult.success(surveyService.listTemplates());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template with full nested structure")
    public ApiResult<SurveyTemplateResponse> get(@PathVariable Long id) {
        return ApiResult.success(surveyService.getTemplate(id));
    }

    @PostMapping
    @Operation(summary = "Create a new survey template")
    public ApiResult<SurveyTemplateResponse> create(@Valid @RequestBody CreateSurveyTemplateRequest request,
                                                     @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.createTemplate(request, user.getUserId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update template metadata")
    public ApiResult<SurveyTemplateResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateSurveyTemplateRequest request,
                                                     @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.updateTemplate(id, request, user.getUserId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a DRAFT template")
    public ApiResult<Void> delete(@PathVariable Long id) {
        surveyService.deleteTemplate(id);
        return ApiResult.success();
    }

    // ── Builder — Pages ──

    @PostMapping("/{templateId}/pages")
    @Operation(summary = "Add a page to a template")
    public ApiResult<SurveyTemplateResponse.PageResponse> addPage(@PathVariable Long templateId,
                                                                    @RequestBody AddPageRequest request,
                                                                    @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.addPage(templateId, request.getTitle(), user.getUserId()));
    }

    @DeleteMapping("/pages/{pageId}")
    @Operation(summary = "Delete a page")
    public ApiResult<Void> deletePage(@PathVariable Long pageId) {
        surveyService.deletePage(pageId);
        return ApiResult.success();
    }

    @PutMapping("/{templateId}/pages/reorder")
    @Operation(summary = "Reorder pages")
    public ApiResult<Void> reorderPages(@PathVariable Long templateId,
                                         @Valid @RequestBody ReorderRequest request) {
        surveyService.reorderPages(templateId, request);
        return ApiResult.success();
    }

    // ── Builder — Sections ──

    @PostMapping("/pages/{pageId}/sections")
    @Operation(summary = "Add a section to a page")
    public ApiResult<SurveyTemplateResponse.SectionResponse> addSection(@PathVariable Long pageId,
                                                                          @RequestBody AddSectionRequest request,
                                                                          @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.addSection(pageId, request.getTitle(), user.getUserId()));
    }

    @DeleteMapping("/sections/{sectionId}")
    @Operation(summary = "Delete a section")
    public ApiResult<Void> deleteSection(@PathVariable Long sectionId) {
        surveyService.deleteSection(sectionId);
        return ApiResult.success();
    }

    // ── Builder — Questions ──

    @PostMapping("/sections/{sectionId}/questions")
    @Operation(summary = "Add a question to a section")
    public ApiResult<SurveyTemplateResponse.QuestionResponse> addQuestion(@PathVariable Long sectionId,
                                                                            @Valid @RequestBody AddQuestionRequest request,
                                                                            @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.addQuestion(sectionId, request, user.getUserId()));
    }

    @PutMapping("/questions/{questionId}")
    @Operation(summary = "Update a question")
    public ApiResult<Void> updateQuestion(@PathVariable Long questionId,
                                           @Valid @RequestBody AddQuestionRequest request,
                                           @AuthenticationPrincipal UserDetailsImpl user) {
        surveyService.updateQuestion(questionId, request, user.getUserId());
        return ApiResult.success();
    }

    @DeleteMapping("/questions/{questionId}")
    @Operation(summary = "Delete a question")
    public ApiResult<Void> deleteQuestion(@PathVariable Long questionId) {
        surveyService.deleteQuestion(questionId);
        return ApiResult.success();
    }

    @PutMapping("/sections/{sectionId}/questions/reorder")
    @Operation(summary = "Reorder questions within a section")
    public ApiResult<Void> reorderQuestions(@PathVariable Long sectionId,
                                             @Valid @RequestBody ReorderRequest request) {
        surveyService.reorderQuestions(sectionId, request);
        return ApiResult.success();
    }

    // ── Builder — Visibility Rules ──

    @PostMapping("/{templateId}/rules")
    @Operation(summary = "Add a visibility rule")
    public ApiResult<SurveyTemplateResponse.VisibilityRuleResponse> addRule(@PathVariable Long templateId,
                                                                              @Valid @RequestBody AddVisibilityRuleRequest request,
                                                                              @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.addVisibilityRule(templateId, request, user.getUserId()));
    }

    @DeleteMapping("/rules/{ruleId}")
    @Operation(summary = "Delete a visibility rule")
    public ApiResult<Void> deleteRule(@PathVariable Long ruleId) {
        surveyService.deleteVisibilityRule(ruleId);
        return ApiResult.success();
    }
}
