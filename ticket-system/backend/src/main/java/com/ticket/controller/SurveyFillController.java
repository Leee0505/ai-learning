package com.ticket.controller;

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
@RequestMapping("/api/surveys")
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Survey Fill", description = "User-facing survey fill endpoints")
public class SurveyFillController {

    private final SurveyService surveyService;

    public SurveyFillController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @GetMapping("/instances")
    @Operation(summary = "List my survey instances")
    public ApiResult<List<SurveyInstanceResponse>> myInstances(@AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.listUserInstances(user.getUserId()));
    }

    @GetMapping("/instances/{id}/fill")
    @Operation(summary = "Get survey fill data (template + answers + hidden states)")
    public ApiResult<SurveyFillResponse> getFillData(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.getFillData(id, user.getUserId()));
    }

    @PutMapping("/instances/{id}/answers")
    @Operation(summary = "Save a single answer (auto-save)")
    public ApiResult<Void> saveAnswer(@PathVariable Long id,
                                       @Valid @RequestBody SaveAnswerRequest request,
                                       @AuthenticationPrincipal UserDetailsImpl user) {
        surveyService.saveAnswer(id, request, user.getUserId());
        return ApiResult.success();
    }

    @PostMapping("/instances/{id}/pages/{pageId}/complete")
    @Operation(summary = "Mark a page as completed")
    public ApiResult<SurveyInstanceResponse> completePage(@PathVariable Long id,
                                                           @PathVariable Long pageId,
                                                           @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.completePage(id, pageId, user.getUserId()));
    }

    @PostMapping("/instances/{id}/pages/{pageId}/reopen")
    @Operation(summary = "Reopen a completed page for editing")
    public ApiResult<SurveyInstanceResponse> reopenPage(@PathVariable Long id,
                                                         @PathVariable Long pageId,
                                                         @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.reopenPage(id, pageId, user.getUserId()));
    }

    @PostMapping("/instances/{id}/submit")
    @Operation(summary = "Complete a submitted survey (requires all pages COMPLETED, instance SUBMITTED)")
    public ApiResult<SurveyInstanceResponse> submit(@PathVariable Long id,
                                                     @Valid @RequestBody SubmitSurveyRequest request,
                                                     @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.submitSurvey(id, request, user.getUserId()));
    }

    @PutMapping("/instances/{id}/reassign")
    @Operation(summary = "Reassign a survey instance to a different user")
    public ApiResult<SurveyInstanceResponse> reassignInstance(@PathVariable Long id,
                                                               @Valid @RequestBody ReassignRequest request,
                                                               @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.reassignInstance(id, request, user.getUserId()));
    }

    @PutMapping("/instances/{id}/pages/{pageId}/reassign")
    @Operation(summary = "Reassign a survey instance page to a different user")
    public ApiResult<Void> reassignPage(@PathVariable Long id,
                                         @PathVariable Long pageId,
                                         @Valid @RequestBody ReassignRequest request,
                                         @AuthenticationPrincipal UserDetailsImpl user) {
        surveyService.reassignPage(id, pageId, request, user.getUserId());
        return ApiResult.success();
    }

    @GetMapping("/users")
    @Operation(summary = "List users available for reassign (same tenant)")
    public ApiResult<PageResponse<UserResponse>> listUsersForReassign(
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal UserDetailsImpl user) {
        UserListRequest req = new UserListRequest();
        req.setSize(size != null ? size : 200);
        req.setPage(1);
        req.setTenantId(user != null ? user.getTenantId() : null);
        return ApiResult.success(surveyService.listUsersForReassign(req));
    }
}
