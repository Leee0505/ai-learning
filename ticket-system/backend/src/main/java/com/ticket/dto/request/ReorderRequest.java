package com.ticket.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReorderRequest {
    @NotNull
    private List<ReorderItem> items;

    @Data
    public static class ReorderItem {
        @NotNull
        private Long id;
        private Integer displayOrder;
    }
}
