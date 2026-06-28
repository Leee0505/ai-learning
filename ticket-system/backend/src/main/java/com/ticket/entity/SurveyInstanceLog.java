package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("survey_instance_log")
public class SurveyInstanceLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Long pageId;
    private Long questionId;
    private String action;
    private Long userId;
    private String detail;
    private Long createdDate;
}
