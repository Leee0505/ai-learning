package com.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating a new support ticket")
public class CreateTicketRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must not exceed 255 characters")
    @Schema(description = "Ticket title — a brief one-line summary of the issue", example = "Unable to reset password", maxLength = 255)
    private String title;

    @NotBlank(message = "description is required")
    @Schema(description = "Detailed description of the issue or request", example = "When I click 'Forgot Password' the reset email never arrives. I have checked spam folder.")
    private String description;

    @NotBlank(message = "priority is required")
    @Schema(description = "Ticket priority level", example = "MEDIUM", allowableValues = {"LOW", "MEDIUM", "HIGH", "URGENT"})
    private String priority;

    @NotBlank(message = "category is required")
    @Schema(description = "Ticket category", example = "ACCOUNT_ISSUE", allowableValues = {"BUG", "FEATURE_REQUEST", "GENERAL_QUESTION", "ACCOUNT_ISSUE", "OTHER"})
    private String category;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
