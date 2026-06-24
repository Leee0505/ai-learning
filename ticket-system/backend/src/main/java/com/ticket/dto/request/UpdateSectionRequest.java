package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateSectionRequest {
    @NotBlank
    private String title;
}
