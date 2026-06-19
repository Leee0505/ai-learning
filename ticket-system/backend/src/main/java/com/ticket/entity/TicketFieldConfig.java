package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("ticket_field_config")
public class TicketFieldConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String name;
    private String fieldKey;
    private String fieldType;
    private String options;
    private Integer required;
    private Integer active;
    private Integer displayOrder;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Long createdDate;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedBy;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedDate;
}
