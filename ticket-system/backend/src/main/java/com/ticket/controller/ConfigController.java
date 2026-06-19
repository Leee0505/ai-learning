package com.ticket.controller;

import com.ticket.common.constant.RoleConstants;
import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/config")
@PreAuthorize("hasRole('" + RoleConstants.ADMIN + "')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Config", description = "Admin ticket configuration — custom fields and SLA rules")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    // ── Custom Fields ──

    @GetMapping("/fields")
    @Operation(summary = "List all custom field definitions")
    public ApiResult<List<FieldConfigResponse>> listFields() {
        return ApiResult.success(configService.listFields());
    }

    @PostMapping("/fields")
    @Operation(summary = "Create a custom field definition")
    public ApiResult<FieldConfigResponse> createField(
            @Valid @RequestBody CreateFieldRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        return ApiResult.success(configService.createField(request, admin.getUserId()));
    }

    @PutMapping("/fields/{id}")
    @Operation(summary = "Update a custom field definition")
    public ApiResult<FieldConfigResponse> updateField(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFieldRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        return ApiResult.success(configService.updateField(id, request, admin.getUserId()));
    }

    @DeleteMapping("/fields/{id}")
    @Operation(summary = "Delete a custom field definition")
    public ApiResult<Void> deleteField(@PathVariable Long id) {
        configService.deleteField(id);
        return ApiResult.success();
    }

    @PutMapping("/fields/reorder")
    @Operation(summary = "Batch reorder custom fields")
    public ApiResult<Void> reorderFields(@Valid @RequestBody ReorderFieldsRequest request) {
        configService.reorderFields(request);
        return ApiResult.success();
    }

    // ── SLA Rules ──

    @GetMapping("/sla")
    @Operation(summary = "List all SLA rules")
    public ApiResult<List<SlaConfigResponse>> listSla() {
        return ApiResult.success(configService.listSla());
    }

    @PutMapping("/sla/{id}")
    @Operation(summary = "Update an SLA rule")
    public ApiResult<SlaConfigResponse> updateSla(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSlaRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        return ApiResult.success(configService.updateSla(id, request, admin.getUserId()));
    }
}
