package com.ticket.controller;

import com.ticket.dto.response.ApiResult;
import com.ticket.entity.Tenant;
import com.ticket.mapper.TenantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Public", description = "Public endpoints for registration and discovery")
public class PublicController {

    private final TenantMapper tenantMapper;

    public PublicController(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    @GetMapping("/tenants")
    @Operation(summary = "List enabled tenants (no auth required)",
               description = "Returns all active tenants for the registration form dropdown. No authentication required.")
    public ApiResult<List<Tenant>> listTenants() {
        List<Tenant> tenants = tenantMapper.selectList(
                new LambdaQueryWrapper<Tenant>()
                        .eq(Tenant::getStatus, 1)
                        .orderByAsc(Tenant::getName));
        return ApiResult.success(tenants);
    }
}
