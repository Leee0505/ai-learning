package com.ticket.dto.response;

import lombok.Data;

@Data
public class FieldConfigResponse {

    private Long id;
    private String name;
    private String fieldKey;
    private String fieldType;
    private String options;
    private Boolean required;
    private Boolean active;
    private Integer displayOrder;
    private Long createdDate;
}
