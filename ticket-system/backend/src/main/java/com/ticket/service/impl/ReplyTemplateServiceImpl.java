package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.common.constant.BusinessConstants;
import com.ticket.common.constant.ErrorCode;
import com.ticket.common.exception.BusinessException;
import com.ticket.dto.request.CreateTemplateRequest;
import com.ticket.dto.response.ReplyTemplateResponse;
import com.ticket.entity.ReplyTemplate;
import com.ticket.mapper.ReplyTemplateMapper;
import com.ticket.service.ReplyTemplateService;
import com.ticket.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        Long tenantId = SecurityUtils.getCurrentTenantId();
        LambdaQueryWrapper<ReplyTemplate> wrapper = new LambdaQueryWrapper<>();
        // System defaults (NULL) + current tenant's templates
        wrapper.and(w -> w.isNull(ReplyTemplate::getTenantId)
                .or().eq(ReplyTemplate::getTenantId, tenantId));
        if (StringUtils.hasText(category)) {
            wrapper.eq(ReplyTemplate::getCategory, category);
        }
        wrapper.orderByAsc(ReplyTemplate::getTitle);
        return templateMapper.selectList(wrapper).stream()
                .map(ReplyTemplateResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public ReplyTemplateResponse createTemplate(CreateTemplateRequest request, Long userId) {
        // Validate title uniqueness within tenant
        Long count = templateMapper.selectCount(
                new LambdaQueryWrapper<ReplyTemplate>()
                        .eq(ReplyTemplate::getTenantId, SecurityUtils.getCurrentTenantId())
                        .eq(ReplyTemplate::getTitle, request.getTitle()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.TEMPLATE_TITLE_DUPLICATE);
        }
        // Validate content length (max 5000 chars)
        if (request.getContent().length() > BusinessConstants.MAX_TEMPLATE_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "content must be under " + BusinessConstants.MAX_TEMPLATE_CONTENT_LENGTH + " characters");
        }

        ReplyTemplate t = new ReplyTemplate();
        t.setTitle(request.getTitle());
        t.setContent(request.getContent());
        t.setCategory(request.getCategory() != null ? request.getCategory() : BusinessConstants.DEFAULT_TEMPLATE_CATEGORY);
        t.setTenantId(SecurityUtils.getCurrentTenantId()); // user's own tenant
        t.setCreatedBy(userId);
        templateMapper.insert(t);
        log.info("Template created: id={} title={} by userId={}", t.getId(), t.getTitle(), userId);
        return ReplyTemplateResponse.from(t);
    }

    @Override
    @Transactional
    public ReplyTemplateResponse updateTemplate(Long id, CreateTemplateRequest request, Long userId) {
        ReplyTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND);
        }
        // System defaults (tenant_id=NULL) only editable by admin
        if (t.getTenantId() == null && !SecurityUtils.isAdmin()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "only admin can edit system default templates");
        }
        // Validate title uniqueness within tenant (exclude self)
        Long count = templateMapper.selectCount(
                new LambdaQueryWrapper<ReplyTemplate>()
                        .eq(ReplyTemplate::getTenantId, SecurityUtils.getCurrentTenantId())
                        .eq(ReplyTemplate::getTitle, request.getTitle())
                        .ne(ReplyTemplate::getId, id));
        if (count > 0) {
            throw new BusinessException(ErrorCode.TEMPLATE_TITLE_DUPLICATE);
        }
        if (request.getContent().length() > BusinessConstants.MAX_TEMPLATE_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "content must be under " + BusinessConstants.MAX_TEMPLATE_CONTENT_LENGTH + " characters");
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
    @Transactional
    public void deleteTemplate(Long id, Long userId) {
        ReplyTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND);
        }
        if (t.getTenantId() == null && !SecurityUtils.isAdmin()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "only admin can delete system default templates");
        }
        templateMapper.deleteById(id);
        log.info("Template deleted: id={} by userId={}", id, userId);
    }

}
