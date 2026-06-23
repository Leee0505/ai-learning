package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.common.constant.BusinessConstants;
import com.ticket.common.constant.ErrorCode;
import com.ticket.common.exception.BusinessException;
import com.ticket.dto.request.CreateTemplateRequest;
import com.ticket.dto.response.ReplyTemplateResponse;
import com.ticket.entity.KnowledgeArticle;
import com.ticket.mapper.KnowledgeArticleMapper;
import com.ticket.service.KnowledgeService;
import com.ticket.util.SecurityUtils;
import org.slf4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeServiceImpl.class);
    private final KnowledgeArticleMapper mapper;

    public KnowledgeServiceImpl(KnowledgeArticleMapper mapper) { this.mapper = mapper; }

    @Override
    public List<ReplyTemplateResponse> search(String keyword, String category) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        Long currentTid = SecurityUtils.getCurrentTenantIdOrNull();
        if (currentTid == null) {
            // Superadmin — sees all articles across all tenants
        } else {
            // System defaults (NULL) + current tenant's articles
            wrapper.and(w -> w.isNull(KnowledgeArticle::getTenantId)
                    .or().eq(KnowledgeArticle::getTenantId, currentTid));
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(KnowledgeArticle::getTitle, keyword)
                    .or().like(KnowledgeArticle::getContent, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(KnowledgeArticle::getCategory, category);
        }
        wrapper.orderByDesc(KnowledgeArticle::getCreatedDate);
        return mapper.selectList(wrapper).stream().map(a -> {
            var r = new ReplyTemplateResponse();
            r.setId(a.getId());
            r.setTitle(a.getTitle());
            r.setContent(a.getContent()); // full content for detail
            r.setCategory(a.getCategory());
            r.setCreatedDate(a.getCreatedDate());
            return r;
        }).toList();
    }

    @Override
    @Transactional
    public ReplyTemplateResponse getById(Long id) {
        KnowledgeArticle a = mapper.selectById(id);
        if (a == null) throw new BusinessException(ErrorCode.KNOWLEDGE_NOT_FOUND);
        // Atomic increment via SQL to avoid view count race condition
        var updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getId, id)
                .setSql("view_count = view_count + 1");
        mapper.update(null, updateWrapper);
        a.setViewCount(a.getViewCount() + 1); // reflect in returned object
        var r = new ReplyTemplateResponse();
        r.setId(a.getId());
        r.setTitle(a.getTitle());
        r.setContent(a.getContent());
        r.setCategory(a.getCategory());
        r.setCreatedDate(a.getCreatedDate());
        return r;
    }

    @Override
    @Transactional
    public ReplyTemplateResponse create(CreateTemplateRequest request, Long userId) {
        // Validate title uniqueness within tenant
        LambdaQueryWrapper<KnowledgeArticle> ckWrapper = new LambdaQueryWrapper<>();
        SecurityUtils.tenantEqOrNull(ckWrapper, KnowledgeArticle::getTenantId, SecurityUtils.getCurrentTenantIdOrNull());
        ckWrapper.eq(KnowledgeArticle::getTitle, request.getTitle());
        Long count = mapper.selectCount(ckWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.REPLY_TEMPLATE_TITLE_DUPLICATE);
        }
        if (request.getContent().length() > BusinessConstants.MAX_KNOWLEDGE_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "content must be under " + BusinessConstants.MAX_KNOWLEDGE_CONTENT_LENGTH + " characters");
        }

        KnowledgeArticle a = new KnowledgeArticle();
        a.setTitle(request.getTitle());
        a.setContent(request.getContent());
        a.setCategory(request.getCategory() != null ? request.getCategory() : BusinessConstants.DEFAULT_TEMPLATE_CATEGORY);
        a.setTenantId(SecurityUtils.getCurrentTenantIdOrNull()); // null for superadmin
        a.setCreatedBy(userId);
        mapper.insert(a);
        log.info("Knowledge article created: id={} title={}", a.getId(), a.getTitle());
        var r = new ReplyTemplateResponse();
        r.setId(a.getId()); r.setTitle(a.getTitle()); r.setContent(a.getContent());
        r.setCategory(a.getCategory()); r.setCreatedDate(a.getCreatedDate());
        return r;
    }

    @Override
    @Transactional
    public ReplyTemplateResponse update(Long id, CreateTemplateRequest request, Long userId) {
        KnowledgeArticle a = mapper.selectById(id);
        if (a == null) throw new BusinessException(ErrorCode.KNOWLEDGE_NOT_FOUND);
        if (a.getTenantId() == null && SecurityUtils.getCurrentTenantIdOrNull() != null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "only admin can edit system default articles");
        }
        // Validate title uniqueness within tenant (exclude self)
        LambdaQueryWrapper<KnowledgeArticle> upWrapper = new LambdaQueryWrapper<>();
        SecurityUtils.tenantEqOrNull(upWrapper, KnowledgeArticle::getTenantId, SecurityUtils.getCurrentTenantIdOrNull());
        upWrapper.eq(KnowledgeArticle::getTitle, request.getTitle());
        upWrapper.ne(KnowledgeArticle::getId, id);
        Long count = mapper.selectCount(upWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.REPLY_TEMPLATE_TITLE_DUPLICATE);
        }
        if (request.getContent().length() > BusinessConstants.MAX_KNOWLEDGE_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "content must be under " + BusinessConstants.MAX_KNOWLEDGE_CONTENT_LENGTH + " characters");
        }

        a.setTitle(request.getTitle());
        a.setContent(request.getContent());
        if (request.getCategory() != null) a.setCategory(request.getCategory());
        mapper.updateById(a);
        var r = new ReplyTemplateResponse();
        r.setId(a.getId()); r.setTitle(a.getTitle()); r.setContent(a.getContent());
        r.setCategory(a.getCategory()); r.setCreatedDate(a.getCreatedDate());
        return r;
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        KnowledgeArticle a = mapper.selectById(id);
        if (a == null) throw new BusinessException(ErrorCode.KNOWLEDGE_NOT_FOUND);
        if (a.getTenantId() == null && SecurityUtils.getCurrentTenantIdOrNull() != null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "only admin can delete system default articles");
        }
        mapper.deleteById(id);
    }

}
