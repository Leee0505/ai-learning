package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_instance_page")
public class SurveyInstancePage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long instanceId;
    private Long pageId;
    private String status;
}
