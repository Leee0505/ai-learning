package com.ticket.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateFieldRequest {

    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    @Pattern(regexp = "^(TEXT|SINGLE_SELECT|NUMBER|DATE)$", message = "fieldType must be one of TEXT, SINGLE_SELECT, NUMBER, DATE")
    private String fieldType;

    @Size(max = 1000, message = "options must not exceed 1000 characters")
    private String options;
    private Boolean required;
    private Boolean active;
    private Integer displayOrder;
}
