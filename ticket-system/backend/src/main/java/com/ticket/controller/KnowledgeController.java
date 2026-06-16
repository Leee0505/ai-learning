package com.ticket.controller;

import com.ticket.common.constant.RoleConstants;
import com.ticket.dto.request.CreateTemplateRequest;
import com.ticket.dto.response.ApiResult;
import com.ticket.dto.response.ReplyTemplateResponse;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('" + RoleConstants.AGENT + "', '" + RoleConstants.ADMIN + "')")
    @Operation(summary = "Search knowledge articles")
    public ApiResult<List<ReplyTemplateResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return ApiResult.success(knowledgeService.search(keyword, category));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('" + RoleConstants.AGENT + "', '" + RoleConstants.ADMIN + "')")
    @Operation(summary = "Get article detail (increments view count)")
    public ApiResult<ReplyTemplateResponse> getById(@PathVariable Long id) {
        return ApiResult.success(knowledgeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + RoleConstants.AGENT + "', '" + RoleConstants.ADMIN + "')")
    @Operation(summary = "Create a knowledge article")
    public ApiResult<ReplyTemplateResponse> create(
            @Valid @RequestBody CreateTemplateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ApiResult.success(knowledgeService.create(request, userDetails.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('" + RoleConstants.AGENT + "', '" + RoleConstants.ADMIN + "')")
    @Operation(summary = "Update a knowledge article")
    public ApiResult<ReplyTemplateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateTemplateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ApiResult.success(knowledgeService.update(id, request, userDetails.getUserId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('" + RoleConstants.AGENT + "', '" + RoleConstants.ADMIN + "')")
    @Operation(summary = "Delete a knowledge article")
    public ApiResult<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        knowledgeService.delete(id, userDetails.getUserId());
        return ApiResult.success(null);
    }
}
