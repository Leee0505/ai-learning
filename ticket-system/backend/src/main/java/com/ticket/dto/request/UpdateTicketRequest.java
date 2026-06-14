package com.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for partially updating a ticket — only provided fields are changed")
public class UpdateTicketRequest {

    @Size(max = 255, message = "title must not exceed 255 characters")
    @Schema(description = "Updated ticket title", example = "Updated: Unable to reset password", maxLength = 255)
    private String title;

    @Schema(description = "Updated ticket description", example = "After further investigation, the issue also occurs with the 'Reset via SMS' option.")
    private String description;

    @Schema(description = "Updated priority", example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH", "URGENT"})
    private String priority;

    @Schema(description = "Updated category", example = "BUG", allowableValues = {"BUG", "FEATURE_REQUEST", "GENERAL_QUESTION", "ACCOUNT_ISSUE", "OTHER"})
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
