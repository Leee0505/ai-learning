package com.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for updating own reply content")
public class UpdateReplyRequest {

    @NotBlank(message = "content is required")
    @Schema(description = "Updated reply content — supports Markdown", example = "Updated: the issue has been resolved in v2.1.")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
