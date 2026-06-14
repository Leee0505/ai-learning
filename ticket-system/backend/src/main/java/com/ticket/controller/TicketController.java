package com.ticket.controller;

import com.ticket.common.constant.RoleConstants;
import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.TicketService;
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

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // ── Create ──

    @PostMapping("/tickets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketDetailResponse response = ticketService.createTicket(request, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── List ──

    @GetMapping("/tickets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> listTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PageResponse<TicketResponse> response = ticketService.listTickets(
                status, priority, category, keyword, page, size,
                userDetails.getUserId(), userDetails.getRole());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Detail ──

    @GetMapping("/tickets/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getTicketDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketDetailResponse response = ticketService.getTicketDetail(
                id, userDetails.getUserId(), userDetails.getRole());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Update ──

    @PutMapping("/tickets/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketDetailResponse response = ticketService.updateTicket(
                id, request, userDetails.getUserId(), userDetails.getRole());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Delete ──

    @DeleteMapping("/tickets/{id}")
    @PreAuthorize("hasRole('" + RoleConstants.ADMIN + "')")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ── Change Status ──

    @PatchMapping("/tickets/{id}/status")
    @PreAuthorize("hasAnyRole('" + RoleConstants.ADMIN + "', '" + RoleConstants.AGENT + "')")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketDetailResponse response = ticketService.changeStatus(
                id, request, userDetails.getUserId(), userDetails.getRole());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Assign ──

    @PatchMapping("/tickets/{id}/assign")
    @PreAuthorize("hasAnyRole('" + RoleConstants.ADMIN + "', '" + RoleConstants.AGENT + "')")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketDetailResponse response = ticketService.assignTicket(
                id, request, userDetails.getUserId(), userDetails.getRole());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Reply ──

    @PostMapping("/tickets/{id}/replies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TicketReplyResponse>> addReply(
            @PathVariable Long id,
            @Valid @RequestBody CreateReplyRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketReplyResponse response = ticketService.addReply(id, request, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Upload Attachment ──

    @PostMapping("/tickets/{id}/attachments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TicketAttachmentResponse>> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketAttachmentResponse response = ticketService.uploadAttachment(id, file, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Download Attachment ──

    @GetMapping("/attachments/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) {
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
}