package com.ticket.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.common.constant.RoleConstants;
import com.ticket.dto.response.ApiResult;
import com.ticket.dto.response.UserResponse;
import com.ticket.entity.User;
import com.ticket.mapper.UserMapper;
import com.ticket.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agents")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Agents", description = "List agents for assignment dropdowns")
public class AgentController {

    private final UserMapper userMapper;

    public AgentController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping
    @Operation(summary = "List all active agents (tenant-scoped)",
               description = "Returns enabled users with ROLE_AGENT, ordered by username. Used for ticket assignment dropdowns and filtering.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of agent users (id + username)"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PreAuthorize("isAuthenticated()")
    public ApiResult<List<UserResponse>> listAgents() {
        var wrapper = new LambdaQueryWrapper<User>();
        // Superadmin (tenantId=null) sees all agents; tenant users see only their own tenant
        Long currentTid = SecurityUtils.getCurrentTenantIdOrNull();
        if (currentTid != null) {
            wrapper.eq(User::getTenantId, currentTid);
        }
        var agents = userMapper.selectList(wrapper
                .eq(User::getRole, RoleConstants.ROLE_AGENT)
                .eq(User::getStatus, 1) // only enabled agents
                .orderByAsc(User::getUsername))
                .stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return ApiResult.success(agents);
    }
}
