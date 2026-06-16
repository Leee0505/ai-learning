package com.ticket.service;

import com.ticket.dto.request.CreateTemplateRequest;
import com.ticket.dto.response.ReplyTemplateResponse;

import java.util.List;

public interface ReplyTemplateService {
    List<ReplyTemplateResponse> listTemplates(String category);
    ReplyTemplateResponse createTemplate(CreateTemplateRequest request, Long userId);
    ReplyTemplateResponse updateTemplate(Long id, CreateTemplateRequest request, Long userId);
    void deleteTemplate(Long id, Long userId);
}
