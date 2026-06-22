package com.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated user list query with optional filters")
public class UserListRequest {

    @Schema(description = "Page number (1-based)", example = "1")
    private Integer page;

    @Schema(description = "Page size", example = "20")
    private Integer size;

    @Schema(description = "Search keyword — matches username, email, or phone", example = "admin")
    private String keyword;

    @Schema(description = "Filter by role", example = "ROLE_AGENT", allowableValues = {"ROLE_USER", "ROLE_AGENT", "ROLE_ADMIN"})
    private String role;

    @Schema(description = "Filter by status: 1 = enabled, 0 = disabled", example = "1")
    private Integer status;

    @Schema(description = "Filter by tenant ID")
    private Long tenantId;

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
