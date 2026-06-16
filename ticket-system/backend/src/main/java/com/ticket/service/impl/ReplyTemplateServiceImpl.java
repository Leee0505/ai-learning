package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.common.constant.ErrorCode;
import com.ticket.common.exception.BusinessException;
import com.ticket.dto.request.CreateTemplateRequest;
import com.ticket.dto.response.ReplyTemplateResponse;
import com.ticket.entity.ReplyTemplate;
import com.ticket.mapper.ReplyTemplateMapper;
import com.ticket.service.ReplyTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ReplyTemplateServiceImpl implements ReplyTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ReplyTemplateServiceImpl.class);

    private final ReplyTemplateMapper templateMapper;

    public ReplyTemplateServiceImpl(ReplyTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    @Override
    public List<ReplyTemplateResponse> listTemplates(String category) {
        LambdaQueryWrapper<ReplyTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            wrapper.eq(ReplyTemplate::getCategory, category);
        }
        wrapper.orderByAsc(ReplyTemplate::getTitle);
        return templateMapper.selectList(wrapper).stream()
                .map(ReplyTemplateResponse::from)
                .toList();
    }

    @Override
    public ReplyTemplateResponse createTemplate(CreateTemplateRequest request, Long userId) {
        ReplyTemplate t = new ReplyTemplate();
        t.setTitle(request.getTitle());
        t.setContent(request.getContent());
        t.setCategory(request.getCategory() != null ? request.getCategory() : "GENERAL");
        t.setCreatedBy(userId);
        templateMapper.insert(t);
        log.info("Template created: id={} title={} by userId={}", t.getId(), t.getTitle(), userId);
        return ReplyTemplateResponse.from(t);
    }

    @Override
    public ReplyTemplateResponse updateTemplate(Long id, CreateTemplateRequest request, Long userId) {
        ReplyTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND);
        }
        t.setTitle(request.getTitle());
        t.setContent(request.getContent());
        if (request.getCategory() != null) {
            t.setCategory(request.getCategory());
        }
        templateMapper.updateById(t);
        log.info("Template updated: id={} by userId={}", id, userId);
        return ReplyTemplateResponse.from(t);
    }

    @Override
    public void deleteTemplate(Long id, Long userId) {
        ReplyTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND);
        }
        templateMapper.deleteById(id);
        log.info("Template deleted: id={} by userId={}", id, userId);
    }
}
