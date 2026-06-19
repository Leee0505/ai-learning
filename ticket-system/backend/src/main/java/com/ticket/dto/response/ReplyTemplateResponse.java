package com.ticket.dto.response;

import com.ticket.entity.ReplyTemplate;

public class ReplyTemplateResponse {

    private Long id;
    private String title;
    private String content;
    private String category;
    private Long tenantId;
    private Long createdDate;

    public static ReplyTemplateResponse from(ReplyTemplate t) {
        ReplyTemplateResponse r = new ReplyTemplateResponse();
        r.id = t.getId();
        r.title = t.getTitle();
        r.content = t.getContent();
        r.category = t.getCategory();
        r.tenantId = t.getTenantId();
        r.createdDate = t.getCreatedDate();
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getCreatedDate() { return createdDate; }
    public void setCreatedDate(Long createdDate) { this.createdDate = createdDate; }
}
