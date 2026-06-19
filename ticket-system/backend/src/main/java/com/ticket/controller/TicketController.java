package com.ticket.controller;

import com.ticket.common.constant.RoleConstants;
import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Ticket", description = "Ticket CRUD, status transitions, assignment, replies, and attachments")
@SecurityRequirement(name = "Bearer Authentication")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // ── Create ──

    @PostMapping("/tickets")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Create a new ticket",
        description = "Submits a new support ticket with title, description, priority, and category. "
                    + "The authenticated user becomes the ticket owner. Status defaults to OPEN."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error — missing required fields"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ApiResult<TicketDetailResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketDetailResponse response = ticketService.createTicket(request, userDetails.getUserId());
        return ApiResult.success(response);
    }

    // ── Dashboard Stats ──

    @GetMapping("/tickets/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get dashboard statistics",
        description = "Returns ticket counts grouped by status. "
                    + "Regular users see only their own tickets; agents/admins see all."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard statistics"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ApiResult<DashboardStatsResponse> getDashboardStats(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        DashboardStatsResponse stats = ticketService.getDashboardStats(
                userDetails.getUserId(), userDetails.getRole());
        return ApiResult.success(stats);
    }

    // ── Agent Performance Stats ──

    @GetMapping(value = "/tickets/stats/agent")
    @PreAuthorize("hasAnyRole('" + com.ticket.common.constant.RoleConstants.AGENT + "', '" + com.ticket.common.constant.RoleConstants.ADMIN + "')")
    @Operation(
        summary = "Get agent performance statistics",
        description = "Returns today's completed count, average processing time, pending queue size, and 7-day completion chart for the current agent."
    )
    public ApiResult<com.ticket.dto.response.AgentStatsResponse> getAgentStats(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ApiResult.success(ticketService.getAgentStats(userDetails.getUserId()));
    }

    // ── Export ──

    @GetMapping("/tickets/export")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Export tickets as CSV or Excel",
        description = "Exports all matching tickets (ignores pagination). "
                    + "Supports the same filters as the list endpoint."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File download"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Resource> exportTickets(
            @Parameter(description = "Export format: csv or excel")
            @RequestParam(defaultValue = "csv") String format,
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by priority")
            @RequestParam(required = false) String priority,
            @Parameter(description = "Filter by category")
            @RequestParam(required = false) String category,
            @Parameter(description = "Search keyword")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by assignee")
            @RequestParam(required = false) String assignedTo,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Resource resource = ticketService.exportTickets(
                format, status, priority, category, keyword, assignedTo,
                userDetails.getUserId(), userDetails.getRole());
        String filename = "tickets-" + java.time.LocalDate.now() + "." + (format.equals("excel") ? "xlsx" : "csv");
        MediaType mediaType = format.equals("excel")
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.parseMediaType("text/csv");
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .body(resource);
    }

    // ── List ──

    @GetMapping("/tickets")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "List tickets with filters",
        description = "Returns a paginated list of tickets. Regular users see only their own tickets; "
                    + "agents and admins see all tickets. Supports filtering by status, priority, category, and keyword search on title."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated ticket list"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ApiResult<PageResponse<TicketResponse>> listTickets(
            @Parameter(description = "Filter by ticket status: OPEN, IN_PROGRESS, RESOLVED, CLOSED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by priority: LOW, MEDIUM, HIGH, URGENT")
            @RequestParam(required = false) String priority,
            @Parameter(description = "Filter by category: BUG, FEATURE_REQUEST, GENERAL_QUESTION, ACCOUNT_ISSUE, OTHER")
            @RequestParam(required = false) String category,
            @Parameter(description = "Search keyword — matches against ticket title (LIKE)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by assignee: 'unassigned' for unclaimed tickets, or a specific user ID")
            @RequestParam(required = false) String assignedTo,
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size (1-100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort order for created date: asc or desc (default)")
            @RequestParam(defaultValue = "desc") String sortOrder,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PageResponse<TicketResponse> response = ticketService.listTickets(
                status, priority, category, keyword, assignedTo, page, size,
                userDetails.getUserId(), userDetails.getRole(), sortOrder);
        return ApiResult.success(response);
    }

    // ── Detail ──

    @GetMapping("/tickets/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get ticket detail",
        description = "Returns the full ticket detail including reply timeline and attachment list. "
                    + "Internal notes (isInternal=true) are hidden from regular users."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket detail with replies and attachments"),
        @ApiResponse(responseCode = "403", description = "Access denied — user does not own this ticket"),
        @ApiResponse(responseCode = "404", description = "Ticket not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ApiResult<TicketDetailResponse> getTicketDetail(
            @Parameter(description = "Ticket ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketDetailResponse response = ticketService.getTicketDetail(
                id, userDetails.getUserId(), userDetails.getRole());
        return ApiResult.success(response);
    }

    // ── Update ──

    @PutMapping("/tickets/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Update ticket fields",
        description = "Partially updates a ticket's title, description, priority, or category. "
                    + "Only the ticket owner (or an agent/admin) can update. Null/blank fields are ignored."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket updated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ApiResult<TicketDetailResponse> updateTicket(
            @Parameter(description = "Ticket ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketDetailResponse response = ticketService.updateTicket(
                id, request, userDetails.getUserId(), userDetails.getRole());
        return ApiResult.success(response);
    }

    // ── Batch Delete ──

    @DeleteMapping("/tickets/batch")
    @PreAuthorize("hasRole('" + RoleConstants.ADMIN + "')")
    @Operation(
        summary = "Batch delete tickets (admin only)",
        description = "Deletes multiple tickets by their IDs. Attachments are cleaned up before deletion."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tickets deleted — returns count of deleted records"),
        @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role")
    })
    public ApiResult<Integer> deleteBatchTickets(
            @Parameter(description = "List of ticket IDs to delete", required = true)
            @RequestBody List<Long> ids) {
        int deleted = ticketService.deleteBatchTickets(ids);
        return ApiResult.success(deleted);
    }

    // ── Delete ──

    @DeleteMapping("/tickets/{id}")
    @PreAuthorize("hasRole('" + RoleConstants.ADMIN + "')")
    @Operation(
        summary = "Delete a ticket (admin only)",
        description = "Permanently deletes the ticket, its replies, and its attachments (DB cascade + disk cleanup). "
                    + "Only available to ROLE_ADMIN."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket deleted"),
        @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ApiResult<Void> deleteTicket(
            @Parameter(description = "Ticket ID", required = true)
            @PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ApiResult.success();
    }

    // ── Change Status ──

    @PatchMapping("/tickets/{id}/status")
    @PreAuthorize("hasAnyRole('" + RoleConstants.ADMIN + "', '" + RoleConstants.AGENT + "')")
    @Operation(
        summary = "Change ticket status",
        description = "Transitions a ticket to a new status. Valid transitions: "
                    + "OPEN → IN_PROGRESS | CLOSED, "
                    + "IN_PROGRESS → RESOLVED | CLOSED, "
                    + "RESOLVED → CLOSED. "
                    + "Requires ADMIN or AGENT role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN or AGENT role"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ApiResult<TicketDetailResponse> changeStatus(
            @Parameter(description = "Ticket ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketDetailResponse response = ticketService.changeStatus(
                id, request, userDetails.getUserId(), userDetails.getRole());
        return ApiResult.success(response);
    }

    // ── Overdue ──

    @GetMapping("/tickets/overdue")
    @PreAuthorize("hasAnyRole('" + RoleConstants.ADMIN + "', '" + RoleConstants.AGENT + "')")
    @Operation(summary = "List overdue tickets",
               description = "Returns OPEN/IN_PROGRESS tickets that have exceeded their SLA response or resolution deadline. "
                           + "Filtered by role: users see own, agents see assigned+unassigned, admins see all.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Overdue tickets returned (may be empty)"),
        @ApiResponse(responseCode = "403", description = "Requires AGENT or ADMIN role")
    })
    public ApiResult<List<TicketDetailResponse>> getOverdue(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ApiResult.success(ticketService.getOverdueTickets(userDetails.getUserId(), userDetails.getRole()));
    }

    // ── Assign ──

    @PatchMapping("/tickets/{id}/assign")
    @PreAuthorize("hasAnyRole('" + RoleConstants.ADMIN + "', '" + RoleConstants.AGENT + "')")
    @Operation(
        summary = "Assign ticket to an agent",
        description = "Assigns (or reassigns) a ticket to a specific agent user. "
                    + "The target user must have the ROLE_AGENT role. Requires ADMIN or AGENT role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket assigned successfully"),
        @ApiResponse(responseCode = "400", description = "Assignment target must be an agent"),
        @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN or AGENT role"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ApiResult<TicketDetailResponse> assignTicket(
            @Parameter(description = "Ticket ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketDetailResponse response = ticketService.assignTicket(
                id, request, userDetails.getUserId(), userDetails.getRole());
        return ApiResult.success(response);
    }

    // ── Reply ──

    @PostMapping("/tickets/{id}/replies")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Add a reply to a ticket",
        description = "Adds a public reply or an internal note (agent-only). "
                    + "Internal notes (isInternal=true) are hidden from the ticket owner (regular user) in the detail view."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reply added"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ApiResult<TicketReplyResponse> addReply(
            @Parameter(description = "Ticket ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CreateReplyRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketReplyResponse response = ticketService.addReply(id, request, userDetails.getUserId());
        return ApiResult.success(response);
    }

    // ── Edit Reply ──

    @PutMapping("/tickets/{id}/replies/{replyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Edit own reply",
        description = "Updates the content of a reply. Only the reply author can edit."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reply updated"),
        @ApiResponse(responseCode = "403", description = "Not the reply author"),
        @ApiResponse(responseCode = "404", description = "Reply not found")
    })
    public ApiResult<TicketReplyResponse> editReply(
            @Parameter(description = "Ticket ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Reply ID", required = true)
            @PathVariable Long replyId,
            @Valid @RequestBody com.ticket.dto.request.UpdateReplyRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketReplyResponse response = ticketService.editReply(id, replyId, request, userDetails.getUserId());
        return ApiResult.success(response);
    }

    // ── Delete Reply ──

    @DeleteMapping("/tickets/{id}/replies/{replyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Delete reply",
        description = "Deletes a reply. Reply author or admin can delete."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reply deleted"),
        @ApiResponse(responseCode = "403", description = "Not authorized to delete"),
        @ApiResponse(responseCode = "404", description = "Reply not found")
    })
    public ApiResult<Void> deleteReply(
            @Parameter(description = "Ticket ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Reply ID", required = true)
            @PathVariable Long replyId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ticketService.deleteReply(id, replyId, userDetails.getUserId(), userDetails.getRole());
        return ApiResult.success(null);
    }

    // ── Upload Attachment ──

    @PostMapping(value = "/tickets/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Upload an attachment to a ticket",
        description = "Uploads a file as an attachment to the specified ticket. "
                    + "Maximum file size is 10 MB. Allowed types include images, documents, archives, and videos."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "File too large (>10 MB) or unsupported file type"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ApiResult<TicketAttachmentResponse> uploadAttachment(
            @Parameter(description = "Ticket ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "File to upload (max 10 MB)", required = true)
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketAttachmentResponse response = ticketService.uploadAttachment(id, file, userDetails.getUserId());
        return ApiResult.success(response);
    }

    // ── Download Attachment ──

    @GetMapping("/attachments/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Download an attachment",
        description = "Downloads an attachment file by its ID. Returns the file as a binary stream "
                    + "with Content-Disposition header for browser download."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File download",
                     content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
        @ApiResponse(responseCode = "400", description = "Attachment not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Resource> downloadAttachment(
            @Parameter(description = "Attachment ID", required = true)
            @PathVariable Long id) {
        Resource resource = ticketService.downloadAttachment(id);
        // Determine Content-Disposition filename from the attachment
        String filename = resource.getFilename();
        String encodedFilename = URLEncoder.encode(filename != null ? filename : "file",
                StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    // ── Thumbnail ──

    @GetMapping("/attachments/{id}/thumbnail")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get attachment thumbnail",
        description = "Returns a resized thumbnail (JPEG) of the attachment. "
                    + "Only works for image attachments (image/*). "
                    + "Uses Lanczos3 interpolation with EXIF orientation correction."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thumbnail image",
                     content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE)),
        @ApiResponse(responseCode = "400", description = "Attachment not found or not an image"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Resource> getThumbnail(
            @Parameter(description = "Attachment ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Thumbnail max dimension in pixels (default 200)")
            @RequestParam(defaultValue = "200") int size) {
        Resource original = ticketService.downloadAttachment(id);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            net.coobird.thumbnailator.Thumbnails.of(original.getInputStream())
                    .size(size, size)
                    .outputFormat("jpg")
                    .toOutputStream(out);
            byte[] thumbBytes = out.toByteArray();
            org.springframework.core.io.ByteArrayResource thumbResource =
                    new org.springframework.core.io.ByteArrayResource(thumbBytes);
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
                    .body(thumbResource);
        } catch (Exception e) {
            throw new com.ticket.common.exception.BusinessException(
                    com.ticket.common.constant.ErrorCode.TICKET_ATTACHMENT_NOT_FOUND);
        }
    }
}
