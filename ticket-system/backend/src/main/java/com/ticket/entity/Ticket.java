package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("ticket")
public class Ticket {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String category;
    private Long assignedTo;
    private Long resolvedDate;
    private Long closedDate;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Long createdDate;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedBy;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedDate;
}
