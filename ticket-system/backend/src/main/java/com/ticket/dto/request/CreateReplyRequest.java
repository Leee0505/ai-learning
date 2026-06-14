package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CreateReplyRequest {
    @NotBlank(message = "content is required")
    private String content;

    private boolean isInternal;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean getIsInternal() { return isInternal; }
    public void setIsInternal(boolean isInternal) { this.isInternal = isInternal; }
}