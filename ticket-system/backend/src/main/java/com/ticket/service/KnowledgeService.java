package com.ticket.service;

import com.ticket.dto.request.CreateTemplateRequest;
import com.ticket.dto.response.ReplyTemplateResponse;

import java.util.List;

public interface KnowledgeService {
    List<ReplyTemplateResponse> search(String keyword, String category);
    ReplyTemplateResponse getById(Long id);
    ReplyTemplateResponse create(CreateTemplateRequest request, Long userId);
    ReplyTemplateResponse update(Long id, CreateTemplateRequest request, Long userId);
    void delete(Long id, Long userId);
}
