package com.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for updating a user's profile fields")
public class UserUpdateRequest {

    @Size(min = 2, max = 64, message = "username must be between 2 and 64 characters")
    @Schema(description = "Updated username", example = "john_doe")
    private String username;

    @Email(message = "email must be a valid format")
    @Schema(description = "Updated email address", example = "john@example.com")
    private String email;

    @Size(max = 32, message = "phone must not exceed 32 characters")
    @Schema(description = "Updated phone number", example = "13800138000")
    private String phone;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
