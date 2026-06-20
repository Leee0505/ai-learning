package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_section")
public class SurveySection {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long pageId;
    private String title;
    private String description;
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
