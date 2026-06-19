package com.ticket.controller;

import com.ticket.common.constant.RoleConstants;
import com.ticket.dto.response.ApiResult;
import com.ticket.entity.Tenant;
import com.ticket.mapper.TenantMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tenants")
@PreAuthorize("hasRole('" + RoleConstants.ADMIN + "')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Tenant", description = "Admin tenant management")
public class TenantController {

    private final TenantMapper tenantMapper;

    public TenantController(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    @GetMapping
    @Operation(summary = "List all enabled tenants (for assignment dropdowns)")
    public ApiResult<List<Tenant>> list() {
        return ApiResult.success(tenantMapper.selectList(null));
    }
}
