package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CreateTemplateRequest {
    @NotBlank private String title;
    @NotBlank private String content;
    private String category;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
