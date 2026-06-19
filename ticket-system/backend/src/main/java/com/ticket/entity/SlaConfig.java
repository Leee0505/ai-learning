package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("sla_config")
public class SlaConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String priority;
    private Integer responseMinutes;
    private Integer resolutionMinutes;
    private Integer active;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Long createdDate;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedBy;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedDate;
}
