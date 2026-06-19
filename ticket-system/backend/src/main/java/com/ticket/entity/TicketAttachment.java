package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("ticket_attachment")
public class TicketAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long ticketId;
    private Long replyId;
    private String filename;
    private String originalFilename;
    private Long fileSize;
    private String contentType;
    private String storagePath;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Long createdDate;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedBy;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedDate;
}
