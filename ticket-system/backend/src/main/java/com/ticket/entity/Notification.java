package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long userId;
    private String type;
    private Long ticketId;
    private String title;
    private String message;
    private Integer isRead;
    private Long createdDate;
}
