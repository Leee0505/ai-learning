package com.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for adding a reply (public or internal note) to a ticket")
public class CreateReplyRequest {

    @NotBlank(message = "content is required")
    @Schema(description = "Reply content — supports plain text", example = "Thank you for reporting this issue. We are investigating.")
    private String content;

    @Schema(description = "When true, marks this reply as an internal note visible only to agents/admins. Defaults to false.",
            example = "false", defaultValue = "false")
    private boolean isInternal;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean getIsInternal() { return isInternal; }
    public void setIsInternal(boolean isInternal) { this.isInternal = isInternal; }
}
