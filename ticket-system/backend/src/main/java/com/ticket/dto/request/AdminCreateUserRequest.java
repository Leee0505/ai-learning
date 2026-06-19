package com.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for admin creating a new user")
public class AdminCreateUserRequest {

    @NotBlank(message = "username is required")
    @Size(min = 2, max = 64, message = "username must be between 2 and 64 characters")
    @Schema(description = "Username for the new account", example = "john_doe")
    private String username;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid format")
    @Schema(description = "Email address", example = "john@example.com")
    private String email;

    @Size(max = 32, message = "phone must not exceed 32 characters")
    @Schema(description = "Phone number (optional)", example = "13800138000")
    private String phone;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 128, message = "password must be between 6 and 128 characters")
    @Schema(description = "Initial password", example = "Pass@123", minLength = 6, maxLength = 128)
    private String password;

    @Schema(description = "Tenant ID for the new user. Defaults to admin's own tenant.", example = "1")
    private Long tenantId;

    @NotBlank(message = "role is required")
    @Pattern(regexp = "ROLE_USER|ROLE_AGENT|ROLE_ADMIN", message = "role must be one of ROLE_USER, ROLE_AGENT, or ROLE_ADMIN")
    @Schema(description = "Role for the new user", example = "ROLE_AGENT", allowableValues = {"ROLE_USER", "ROLE_AGENT", "ROLE_ADMIN"})
    private String role;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
