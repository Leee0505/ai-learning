package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_template")
public class SurveyTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String title;
    private String description;
    private String status;
    private Integer version;
    private Long originId;
    private Integer allowResubmit;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Long createdDate;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedBy;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedDate;
}
