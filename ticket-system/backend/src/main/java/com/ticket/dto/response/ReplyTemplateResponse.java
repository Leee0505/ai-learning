package com.ticket.dto.response;

import com.ticket.entity.ReplyTemplate;

public class ReplyTemplateResponse {

    private Long id;
    private String title;
    private String content;
    private String category;
    private Long createdDate;

    public static ReplyTemplateResponse from(ReplyTemplate t) {
        ReplyTemplateResponse r = new ReplyTemplateResponse();
        r.id = t.getId();
        r.title = t.getTitle();
        r.content = t.getContent();
        r.category = t.getCategory();
        r.createdDate = t.getCreatedDate();
        return r;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public Long getCreatedDate() { return createdDate; }
}
