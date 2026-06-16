package com.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request body for changing a user's role")
public class UserRoleUpdateRequest {

    @NotBlank(message = "role is required")
    @Pattern(regexp = "ROLE_USER|ROLE_AGENT|ROLE_ADMIN", message = "role must be one of ROLE_USER, ROLE_AGENT, or ROLE_ADMIN")
    @Schema(description = "New role for the user", example = "ROLE_AGENT", allowableValues = {"ROLE_USER", "ROLE_AGENT", "ROLE_ADMIN"})
    private String role;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
