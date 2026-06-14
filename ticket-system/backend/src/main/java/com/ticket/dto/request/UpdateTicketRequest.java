package com.ticket.dto.request;

import jakarta.validation.constraints.Size;

public class UpdateTicketRequest {
    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    private String description;
    private String priority;
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
