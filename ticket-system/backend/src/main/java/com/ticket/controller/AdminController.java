package com.ticket.controller;

import com.ticket.common.constant.RoleConstants;
import com.ticket.dto.request.*;
import com.ticket.dto.response.ApiResult;
import com.ticket.dto.response.PageResponse;
import com.ticket.dto.response.UserResponse;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('" + RoleConstants.ADMIN + "')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin", description = "Admin-only user management and system configuration endpoints")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    @Operation(
        summary = "List all users (paginated)",
        description = "Returns a paginated list of users with optional filters by keyword, role, and status."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated user list"),
        @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role")
    })
    public ApiResult<PageResponse<UserResponse>> listUsers(
            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Search keyword — matches username, email, or phone")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by role")
            @RequestParam(required = false) String role,
            @Parameter(description = "Filter by status: 1 = enabled, 0 = disabled")
            @RequestParam(required = false) Integer status,
            @AuthenticationPrincipal UserDetailsImpl user) {
        UserListRequest request = new UserListRequest();
        request.setPage(page);
        request.setSize(size);
        request.setKeyword(keyword);
        request.setRole(role);
        request.setStatus(status);
        request.setTenantId(user != null ? user.getTenantId() : null);
        return ApiResult.success(adminService.listUsers(request));
    }

    @PostMapping("/users")
    @Operation(
        summary = "Create a new user",
        description = "Creates a new user account with the specified role. Username and email must be unique."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error — username or email already exists"),
        @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role")
    })
    public ApiResult<UserResponse> createUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        return ApiResult.success(adminService.createUser(request, admin.getUserId()));
    }

    @GetMapping("/users/{id}")
    @Operation(
        summary = "Get a single user by ID",
        description = "Returns the full user profile for the given user ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "400", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role")
    })
    public ApiResult<UserResponse> getUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id) {
        return ApiResult.success(adminService.getUserById(id));
    }

    @PutMapping("/users/{id}")
    @Operation(
        summary = "Update a user's profile",
        description = "Partially update a user's username, email, or phone. Only provided fields are changed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated"),
        @ApiResponse(responseCode = "400", description = "Validation error or username/email already taken"),
        @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role")
    })
    public ApiResult<UserResponse> updateUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        return ApiResult.success(adminService.updateUser(id, request, admin.getUserId()));
    }

    @DeleteMapping("/users/{id}")
    @Operation(
        summary = "Delete a user",
        description = "Permanently deletes a user account. Cannot delete your own account."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User deleted"),
        @ApiResponse(responseCode = "400", description = "Cannot delete yourself or user not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role")
    })
    public ApiResult<Void> deleteUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        adminService.deleteUser(id, admin.getUserId());
        return ApiResult.success();
    }

    @PatchMapping("/users/{id}/role")
    @Operation(
        summary = "Change a user's role",
        description = "Updates the user's role to USER, AGENT, or ADMIN. Cannot change your own role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role updated"),
        @ApiResponse(responseCode = "400", description = "Invalid role or cannot change own role"),
        @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role")
    })
    public ApiResult<UserResponse> changeRole(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        return ApiResult.success(adminService.changeRole(id, request, admin.getUserId()));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(
        summary = "Enable or disable a user account",
        description = "Sets the user's status to enabled (1) or disabled (0). Cannot disable your own account."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "400", description = "Cannot disable yourself or user not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role")
    })
    public ApiResult<UserResponse> updateStatus(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        return ApiResult.success(adminService.updateStatus(id, request, admin.getUserId()));
    }
}
