package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_answer")
public class SurveyAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long instanceId;
    private Long questionId;
    private String value;
}
