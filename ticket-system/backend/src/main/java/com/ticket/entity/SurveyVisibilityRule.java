package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_visibility_rule")
public class SurveyVisibilityRule {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;
    private String targetType;
    private Long targetId;
    private Long sourceQuestionId;
    private String op;
    private String value;
    private String logicGroup;
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
