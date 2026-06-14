package com.ticket.controller;

import com.ticket.common.constant.RoleConstants;
import com.ticket.common.constant.SecurityConstants;
import com.ticket.dto.request.*;
import com.ticket.dto.response.ApiResult;
import com.ticket.dto.response.AuthResponse;
import com.ticket.dto.response.UserResponse;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.AuthService;
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
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, login, token refresh, and admin invitation")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user account",
        description = "Creates a new user account with ROLE_USER. Returns an access token and refresh token on success."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration successful — returns JWT tokens"),
        @ApiResponse(responseCode = "400", description = "Validation error — username or email already exists")
    })
    public ApiResult<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResult.success(response);
    }

    @PostMapping("/login")
    @Operation(
        summary = "Login with username or email",
        description = "Authenticates a user by login (username or email) and password. Returns JWT access token (2h) and refresh token (7d)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful — returns JWT tokens"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ApiResult<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResult.success(response);
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Logout and revoke token",
        description = "Adds the current access token to the blacklist and removes the refresh token from the whitelist."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout successful (token invalidated)")
    })
    public ApiResult<Void> logout(
            @Parameter(description = "Bearer JWT token", required = true)
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || authHeader.length() <= SecurityConstants.BEARER_PREFIX_LENGTH
                || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return ApiResult.success();
        }
        String token = authHeader.substring(SecurityConstants.BEARER_PREFIX_LENGTH);
        authService.logout(token);
        return ApiResult.success();
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Exchanges a valid refresh token for a new access token and refresh token pair. "
                    + "The old refresh token is consumed (token rotation)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Refresh token expired, revoked, or invalid")
    })
    public ApiResult<AuthResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refresh(request);
        return ApiResult.success(response);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Get current user profile",
        description = "Returns the authenticated user's profile information (id, username, email, role)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current user profile"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ApiResult<UserResponse> me(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserResponse response = authService.getCurrentUser(userDetails.getUserId();
        return ApiResult.success(response);
    }

    @PostMapping("/invite")
    @PreAuthorize("hasRole('" + RoleConstants.ADMIN + "')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Invite a new agent (admin only)",
        description = "Generates an invitation token for a new agent user. "
                    + "The invite link expires after 48 hours. Requires ROLE_ADMIN."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitation token generated"),
        @ApiResponse(responseCode = "400", description = "Email already has a pending invitation"),
        @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role")
    })
    public ApiResult<String> invite(
            @Valid @RequestBody InviteRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        String token = authService.invite(request, admin.getUserId();
        return ApiResult.success(token);
    }

    @PostMapping("/accept-invite")
    @Operation(
        summary = "Accept an invitation",
        description = "Accepts an invitation token and completes agent registration with username and password."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitation accepted — agent registered with ROLE_AGENT"),
        @ApiResponse(responseCode = "400", description = "Invitation token expired, already used, or not found")
    })
    public ApiResult<AuthResponse> acceptInvite(
            @Valid @RequestBody AcceptInviteRequest request) {
        AuthResponse response = authService.acceptInvite(request);
        return ApiResult.success(response);
    }
}
