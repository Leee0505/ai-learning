package com.ticket.dto.response;

import lombok.Data;

@Data
public class NotificationResponse {
    private Long id;
    private String type;
    private Long ticketId;
    private String title;
    private String message;
    private Boolean isRead;
    private Long createdDate;
}
