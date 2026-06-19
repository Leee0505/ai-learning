package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("knowledge_article")
public class KnowledgeArticle {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String title;
    private String content;
    private String category;
    private String tags;
    private Long viewCount;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Long createdDate;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedBy;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedDate;
}
