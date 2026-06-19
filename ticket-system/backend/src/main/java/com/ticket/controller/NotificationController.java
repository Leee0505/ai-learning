package com.ticket.controller;

import com.ticket.dto.response.ApiResult;
import com.ticket.dto.response.NotificationResponse;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Notifications", description = "Real-time notification endpoints (list, read, unread count)")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List notifications for current user (newest first)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated notification list")
    })
    public ApiResult<List<NotificationResponse>> list(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ApiResult.success(notificationService.listNotifications(user.getUserId(), page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get current unread notification count")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Unread count (may be 0)")
    })
    public ApiResult<Long> getUnreadCount(@AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(notificationService.getUnreadCount(user.getUserId()));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a single notification as read")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification marked as read"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ApiResult<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl user) {
        notificationService.markRead(id, user.getUserId());
        return ApiResult.success();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "All notifications marked as read")
    })
    public ApiResult<Void> markAllRead(@AuthenticationPrincipal UserDetailsImpl user) {
        notificationService.markAllRead(user.getUserId());
        return ApiResult.success();
    }
}
