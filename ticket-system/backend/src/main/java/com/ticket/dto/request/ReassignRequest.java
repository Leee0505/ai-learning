package com.ticket.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReassignRequest {
    @NotNull
    private Long userId;
}
