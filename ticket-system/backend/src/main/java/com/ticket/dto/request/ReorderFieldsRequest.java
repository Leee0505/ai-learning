package com.ticket.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReorderFieldsRequest {

    @Valid
    @NotNull(message = "items must not be null")
    private List<ReorderItem> items;

    @Data
    public static class ReorderItem {
        @NotNull(message = "id is required")
        private Long id;

        @NotNull(message = "displayOrder is required")
        private Integer displayOrder;
    }
}
