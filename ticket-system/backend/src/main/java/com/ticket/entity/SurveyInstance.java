package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_instance")
public class SurveyInstance {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long templateId;
    private String title;
    private String status;
    private Long assignedTo;
    private String triggerType;
    private Long ticketId;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Long createdDate;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedBy;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedDate;
}
