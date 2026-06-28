package com.ticket.dto.response;

import com.ticket.entity.SurveyInstanceLog;
import lombok.Data;

@Data
public class SurveyInstanceLogResponse {
    private Long id;
    private Long instanceId;
    private Long pageId;
    private Long questionId;
    private String action;
    private Long userId;
    private String detail;
    private Long createdDate;

    public static SurveyInstanceLogResponse from(SurveyInstanceLog e) {
        SurveyInstanceLogResponse r = new SurveyInstanceLogResponse();
        r.setId(e.getId());
        r.setInstanceId(e.getInstanceId());
        r.setPageId(e.getPageId());
        r.setQuestionId(e.getQuestionId());
        r.setAction(e.getAction());
        r.setUserId(e.getUserId());
        r.setDetail(e.getDetail());
        r.setCreatedDate(e.getCreatedDate());
        return r;
    }
}
