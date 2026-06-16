package com.ticket.controller;

import com.ticket.common.constant.RoleConstants;
import com.ticket.dto.request.CreateTemplateRequest;
import com.ticket.dto.response.ApiResult;
import com.ticket.dto.response.ReplyTemplateResponse;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.ReplyTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class ReplyTemplateController {

    private final ReplyTemplateService templateService;

    public ReplyTemplateController(ReplyTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('" + RoleConstants.AGENT + "', '" + RoleConstants.ADMIN + "')")
    @Operation(summary = "List reply templates (agent/admin only)")
    public ApiResult<List<ReplyTemplateResponse>> list(
            @Parameter(description = "Filter by category")
            @RequestParam(required = false) String category) {
        return ApiResult.success(templateService.listTemplates(category));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + RoleConstants.AGENT + "', '" + RoleConstants.ADMIN + "')")
    @Operation(summary = "Create a reply template")
    public ApiResult<ReplyTemplateResponse> create(
            @Valid @RequestBody CreateTemplateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ApiResult.success(templateService.createTemplate(request, userDetails.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('" + RoleConstants.AGENT + "', '" + RoleConstants.ADMIN + "')")
    @Operation(summary = "Update a reply template")
    public ApiResult<ReplyTemplateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateTemplateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ApiResult.success(templateService.updateTemplate(id, request, userDetails.getUserId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('" + RoleConstants.AGENT + "', '" + RoleConstants.ADMIN + "')")
    @Operation(summary = "Delete a reply template")
    public ApiResult<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        templateService.deleteTemplate(id, userDetails.getUserId());
        return ApiResult.success(null);
    }
}
