package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("tenant")
public class Tenant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String slug;
    private Integer status;
    private Long createdDate;
}
