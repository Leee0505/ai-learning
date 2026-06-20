package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.common.constant.BusinessConstants;
import com.ticket.common.constant.ErrorCode;
import com.ticket.common.exception.BusinessException;
import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import com.ticket.entity.*;
import com.ticket.mapper.*;
import com.ticket.service.SurveyService;
import com.ticket.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SurveyServiceImpl implements SurveyService {

    private static final Logger log = LoggerFactory.getLogger(SurveyServiceImpl.class);

    private final SurveyTemplateMapper templateMapper;
    private final SurveyPageMapper pageMapper;
    private final SurveySectionMapper sectionMapper;
    private final SurveyQuestionMapper questionMapper;
    private final SurveyVisibilityRuleMapper ruleMapper;

    public SurveyServiceImpl(SurveyTemplateMapper templateMapper, SurveyPageMapper pageMapper,
                             SurveySectionMapper sectionMapper, SurveyQuestionMapper questionMapper,
                             SurveyVisibilityRuleMapper ruleMapper) {
        this.templateMapper = templateMapper;
        this.pageMapper = pageMapper;
        this.sectionMapper = sectionMapper;
        this.questionMapper = questionMapper;
        this.ruleMapper = ruleMapper;
    }

    // ── Template CRUD ──

    @Override
    @Transactional
    public SurveyTemplateResponse createTemplate(CreateSurveyTemplateRequest request, Long adminId) {
        SurveyTemplate t = new SurveyTemplate();
        t.setTenantId(SecurityUtils.getCurrentTenantId());
        t.setTitle(request.getTitle());
        t.setDescription(request.getDescription());
        t.setStatus(BusinessConstants.SURVEY_STATUS_DRAFT);
        t.setVersion(1);
        t.setAllowResubmit(request.getAllowResubmit() != null ? request.getAllowResubmit() : 0);
        t.setCreatedBy(adminId);
        t.setCreatedDate(System.currentTimeMillis());
        templateMapper.insert(t);

        // Create a default first page
        SurveyPage page = new SurveyPage();
        page.setTemplateId(t.getId());
        page.setTitle("Page 1");
        page.setDisplayOrder(1);
        page.setCreatedBy(adminId);
        page.setCreatedDate(System.currentTimeMillis());
        pageMapper.insert(page);

        log.info("Survey template created: id={} title={} tenantId={}", t.getId(), t.getTitle(), t.getTenantId());
        return toTemplateResponse(t);
    }

    @Override
    public SurveyTemplateResponse getTemplate(Long templateId) {
        SurveyTemplate t = findTemplateOrFail(templateId);
        return toTemplateResponse(t);
    }

    @Override
    public List<SurveyTemplateResponse> listTemplates() {
        return templateMapper.selectList(new LambdaQueryWrapper<SurveyTemplate>()
                .orderByDesc(SurveyTemplate::getCreatedDate))
                .stream()
                .map(this::toTemplateResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SurveyTemplateResponse updateTemplate(Long id, UpdateSurveyTemplateRequest request, Long adminId) {
        SurveyTemplate t = findTemplateOrFail(id);
        if (StringUtils.hasText(request.getTitle())) t.setTitle(request.getTitle());
        if (request.getDescription() != null) t.setDescription(request.getDescription());
        t.setLastModifiedBy(adminId);
        t.setLastModifiedDate(System.currentTimeMillis());
        templateMapper.updateById(t);
        return toTemplateResponse(t);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        SurveyTemplate t = findTemplateOrFail(id);
        if (BusinessConstants.SURVEY_STATUS_PUBLISHED.equals(t.getStatus())) {
            throw new BusinessException(ErrorCode.TEMPLATE_ALREADY_PUBLISHED);
        }
        // Cascade delete handled by DB foreign keys
        templateMapper.deleteById(id);
        log.info("Survey template deleted: id={}", id);
    }

    // ── Builder — Pages ──

    @Override
    @Transactional
    public SurveyTemplateResponse.PageResponse addPage(Long templateId, String title, Long adminId) {
        findTemplateOrFail(templateId); // validate exists
        Long count = pageMapper.selectCount(new LambdaQueryWrapper<SurveyPage>()
                .eq(SurveyPage::getTemplateId, templateId));
        SurveyPage page = new SurveyPage();
        page.setTemplateId(templateId);
        page.setTitle(title != null ? title : "Page " + (count + 1));
        page.setDisplayOrder(count.intValue() + 1);
        page.setCreatedBy(adminId);
        page.setCreatedDate(System.currentTimeMillis());
        pageMapper.insert(page);
        return toPageResponse(page);
    }

    @Override
    @Transactional
    public void deletePage(Long pageId) {
        pageMapper.deleteById(pageId); // cascade handled by DB
    }

    @Override
    @Transactional
    public void reorderPages(Long templateId, ReorderRequest request) {
        for (ReorderRequest.ReorderItem item : request.getItems()) {
            SurveyPage page = pageMapper.selectById(item.getId());
            if (page != null && page.getTemplateId().equals(templateId)) {
                page.setDisplayOrder(item.getDisplayOrder());
                pageMapper.updateById(page);
            }
        }
    }

    // ── Builder — Sections ──

    @Override
    @Transactional
    public SurveyTemplateResponse.SectionResponse addSection(Long pageId, String title, Long adminId) {
        Long count = sectionMapper.selectCount(new LambdaQueryWrapper<SurveySection>()
                .eq(SurveySection::getPageId, pageId));
        SurveySection section = new SurveySection();
        section.setPageId(pageId);
        section.setTitle(title != null ? title : "Section " + (count + 1));
        section.setDisplayOrder(count.intValue() + 1);
        section.setCreatedBy(adminId);
        section.setCreatedDate(System.currentTimeMillis());
        sectionMapper.insert(section);
        return toSectionResponse(section);
    }

    @Override
    @Transactional
    public void deleteSection(Long sectionId) {
        sectionMapper.deleteById(sectionId);
    }

    // ── Builder — Questions ──

    @Override
    @Transactional
    public SurveyTemplateResponse.QuestionResponse addQuestion(Long sectionId, AddQuestionRequest request, Long adminId) {
        Long count = questionMapper.selectCount(new LambdaQueryWrapper<SurveyQuestion>()
                .eq(SurveyQuestion::getSectionId, sectionId));
        SurveyQuestion q = new SurveyQuestion();
        q.setSectionId(sectionId);
        q.setType(request.getType());
        q.setTitle(request.getTitle());
        q.setDescription(request.getDescription());
        q.setOptions(request.getOptions());
        q.setRequired(request.getRequired() != null ? request.getRequired() : 0);
        q.setDisplayOrder(count.intValue() + 1);
        q.setCreatedBy(adminId);
        q.setCreatedDate(System.currentTimeMillis());
        questionMapper.insert(q);
        return toQuestionResponse(q);
    }

    @Override
    @Transactional
    public void updateQuestion(Long questionId, AddQuestionRequest request, Long adminId) {
        SurveyQuestion q = questionMapper.selectById(questionId);
        if (q == null) throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND, "question not found");
        if (request.getType() != null) q.setType(request.getType());
        if (request.getTitle() != null) q.setTitle(request.getTitle());
        if (request.getDescription() != null) q.setDescription(request.getDescription());
        if (request.getOptions() != null) q.setOptions(request.getOptions());
        if (request.getRequired() != null) q.setRequired(request.getRequired());
        q.setLastModifiedBy(adminId);
        q.setLastModifiedDate(System.currentTimeMillis());
        questionMapper.updateById(q);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        questionMapper.deleteById(questionId);
    }

    @Override
    @Transactional
    public void reorderQuestions(Long sectionId, ReorderRequest request) {
        for (ReorderRequest.ReorderItem item : request.getItems()) {
            SurveyQuestion q = questionMapper.selectById(item.getId());
            if (q != null && q.getSectionId().equals(sectionId)) {
                q.setDisplayOrder(item.getDisplayOrder());
                questionMapper.updateById(q);
            }
        }
    }

    // ── Builder — Visibility Rules ──

    @Override
    @Transactional
    public SurveyTemplateResponse.VisibilityRuleResponse addVisibilityRule(Long templateId, AddVisibilityRuleRequest request, Long adminId) {
        findTemplateOrFail(templateId);
        SurveyVisibilityRule rule = new SurveyVisibilityRule();
        rule.setTemplateId(templateId);
        rule.setTargetType(request.getTargetType());
        rule.setTargetId(request.getTargetId());
        rule.setSourceQuestionId(request.getSourceQuestionId());
        rule.setOp(request.getOp());
        rule.setValue(request.getValue());
        rule.setLogicGroup(request.getLogicGroup() != null ? String.valueOf(request.getLogicGroup()) : "0");
        rule.setDisplayOrder(1);
        rule.setCreatedBy(adminId);
        rule.setCreatedDate(System.currentTimeMillis());
        ruleMapper.insert(rule);
        return toRuleResponse(rule);
    }

    @Override
    @Transactional
    public void deleteVisibilityRule(Long ruleId) {
        ruleMapper.deleteById(ruleId);
    }

    // ── Private Helpers ──

    private SurveyTemplate findTemplateOrFail(Long id) {
        SurveyTemplate t = templateMapper.selectById(id);
        if (t == null) throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND);
        return t;
    }

    // Build the full nested response structure
    private SurveyTemplateResponse toTemplateResponse(SurveyTemplate t) {
        SurveyTemplateResponse r = new SurveyTemplateResponse();
        r.setId(t.getId());
        r.setTenantId(t.getTenantId());
        r.setTitle(t.getTitle());
        r.setDescription(t.getDescription());
        r.setStatus(t.getStatus());
        r.setVersion(t.getVersion());
        r.setOriginId(t.getOriginId());
        r.setAllowResubmit(t.getAllowResubmit() != null && t.getAllowResubmit() == 1);
        r.setCreatedDate(t.getCreatedDate());

        // Load pages
        List<SurveyPage> pages = pageMapper.selectList(
                new LambdaQueryWrapper<SurveyPage>()
                        .eq(SurveyPage::getTemplateId, t.getId())
                        .orderByAsc(SurveyPage::getDisplayOrder));
        r.setPages(pages.stream().map(this::toPageResponse).collect(Collectors.toList()));
        return r;
    }

    private SurveyTemplateResponse.PageResponse toPageResponse(SurveyPage p) {
        SurveyTemplateResponse.PageResponse pr = new SurveyTemplateResponse.PageResponse();
        pr.setId(p.getId());
        pr.setTitle(p.getTitle());
        pr.setDisplayOrder(p.getDisplayOrder());

        // Page visibility rules
        pr.setVisibilityRules(loadRules(BusinessConstants.SURVEY_TARGET_PAGE, p.getId()));

        // Load sections
        List<SurveySection> sections = sectionMapper.selectList(
                new LambdaQueryWrapper<SurveySection>()
                        .eq(SurveySection::getPageId, p.getId())
                        .orderByAsc(SurveySection::getDisplayOrder));
        pr.setSections(sections.stream().map(this::toSectionResponse).collect(Collectors.toList()));
        return pr;
    }

    private SurveyTemplateResponse.SectionResponse toSectionResponse(SurveySection s) {
        SurveyTemplateResponse.SectionResponse sr = new SurveyTemplateResponse.SectionResponse();
        sr.setId(s.getId());
        sr.setTitle(s.getTitle());
        sr.setDescription(s.getDescription());
        sr.setDisplayOrder(s.getDisplayOrder());

        // Section visibility rules
        sr.setVisibilityRules(loadRules(BusinessConstants.SURVEY_TARGET_SECTION, s.getId()));

        // Load questions
        List<SurveyQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<SurveyQuestion>()
                        .eq(SurveyQuestion::getSectionId, s.getId())
                        .orderByAsc(SurveyQuestion::getDisplayOrder));
        sr.setQuestions(questions.stream().map(this::toQuestionResponse).collect(Collectors.toList()));
        return sr;
    }

    private SurveyTemplateResponse.QuestionResponse toQuestionResponse(SurveyQuestion q) {
        SurveyTemplateResponse.QuestionResponse qr = new SurveyTemplateResponse.QuestionResponse();
        qr.setId(q.getId());
        qr.setType(q.getType());
        qr.setTitle(q.getTitle());
        qr.setDescription(q.getDescription());
        qr.setOptions(q.getOptions());
        qr.setRequired(q.getRequired() != null && q.getRequired() == 1);
        qr.setDisplayOrder(q.getDisplayOrder());

        // Question visibility rules
        qr.setVisibilityRules(loadRules(BusinessConstants.SURVEY_TARGET_QUESTION, q.getId()));
        return qr;
    }

    private List<SurveyTemplateResponse.VisibilityRuleResponse> loadRules(String targetType, Long targetId) {
        List<SurveyVisibilityRule> rules = ruleMapper.selectList(
                new LambdaQueryWrapper<SurveyVisibilityRule>()
                        .eq(SurveyVisibilityRule::getTargetType, targetType)
                        .eq(SurveyVisibilityRule::getTargetId, targetId)
                        .orderByAsc(SurveyVisibilityRule::getLogicGroup, SurveyVisibilityRule::getDisplayOrder));
        return rules.stream().map(this::toRuleResponse).collect(Collectors.toList());
    }

    private SurveyTemplateResponse.VisibilityRuleResponse toRuleResponse(SurveyVisibilityRule rl) {
        SurveyTemplateResponse.VisibilityRuleResponse vr = new SurveyTemplateResponse.VisibilityRuleResponse();
        vr.setId(rl.getId());
        vr.setSourceQuestionId(rl.getSourceQuestionId());
        vr.setOp(rl.getOp());
        vr.setValue(rl.getValue());
        try {
            vr.setLogicGroup(rl.getLogicGroup() != null ? Integer.parseInt(rl.getLogicGroup()) : 0);
        } catch (NumberFormatException e) {
            vr.setLogicGroup(0);
        }
        return vr;
    }
}
