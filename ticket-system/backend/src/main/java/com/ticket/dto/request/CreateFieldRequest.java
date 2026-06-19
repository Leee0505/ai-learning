package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFieldRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "fieldKey is required")
    @Size(max = 50, message = "fieldKey must not exceed 50 characters")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "fieldKey must start with a lowercase letter and contain only lowercase letters, digits, and underscores")
    private String fieldKey;

    @NotBlank(message = "fieldType is required")
    @Pattern(regexp = "^(TEXT|SINGLE_SELECT|NUMBER|DATE)$", message = "fieldType must be one of TEXT, SINGLE_SELECT, NUMBER, DATE")
    private String fieldType;

    @Size(max = 1000, message = "options must not exceed 1000 characters")
    private String options;

    private Boolean required = false;
    private Boolean active = true;
    private Integer displayOrder;
}
