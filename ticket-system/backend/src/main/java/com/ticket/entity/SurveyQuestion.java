package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_question")
public class SurveyQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sectionId;
    private String type;
    private String title;
    private String description;
    private String options;
    private Integer required;
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
