package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.constant.BusinessConstants;
import com.ticket.common.constant.ErrorCode;
import com.ticket.common.exception.BusinessException;
import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import com.ticket.entity.*;
import com.ticket.mapper.*;
import com.ticket.service.SurveyService;
import com.ticket.service.SurveyVisibilityEngine;
import com.ticket.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Map;

@Service
public class SurveyServiceImpl implements SurveyService {

    private static final Logger log = LoggerFactory.getLogger(SurveyServiceImpl.class);

    private final SurveyTemplateMapper templateMapper;
    private final SurveyPageMapper pageMapper;
    private final SurveySectionMapper sectionMapper;
    private final SurveyQuestionMapper questionMapper;
    private final SurveyVisibilityRuleMapper ruleMapper;
    private final SurveyInstanceMapper instanceMapper;
    private final SurveyInstancePageMapper instancePageMapper;
    private final SurveyAnswerMapper answerMapper;
    private final SurveyVisibilityEngine visibilityEngine;
    private final UserMapper userMapper;

    public SurveyServiceImpl(SurveyTemplateMapper templateMapper, SurveyPageMapper pageMapper,
                             SurveySectionMapper sectionMapper, SurveyQuestionMapper questionMapper,
                             SurveyVisibilityRuleMapper ruleMapper, SurveyInstanceMapper instanceMapper,
                             SurveyInstancePageMapper instancePageMapper, SurveyAnswerMapper answerMapper,
                             SurveyVisibilityEngine visibilityEngine,
                             UserMapper userMapper) {
        this.templateMapper = templateMapper;
        this.pageMapper = pageMapper;
        this.sectionMapper = sectionMapper;
        this.questionMapper = questionMapper;
        this.ruleMapper = ruleMapper;
        this.instanceMapper = instanceMapper;
        this.instancePageMapper = instancePageMapper;
        this.answerMapper = answerMapper;
        this.visibilityEngine = visibilityEngine;
        this.userMapper = userMapper;
    }

    // ── Template CRUD ──

    @Override
    @Transactional
    public SurveyTemplateResponse createTemplate(CreateSurveyTemplateRequest request, Long adminId) {
        SurveyTemplate t = new SurveyTemplate();
        t.setTenantId(SecurityUtils.getCurrentTenantIdOrNull()); // null for superadmin = system default
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
        checkTemplateTenantAccess(t);
        return toTemplateResponse(t);
    }

    @Override
    public PageResponse<SurveyTemplateResponse> listTemplates(int page, int size) {
        Long currentTid = SecurityUtils.getCurrentTenantIdOrNull();
        var wrapper = new LambdaQueryWrapper<SurveyTemplate>();
        // Tenant-scoped users see their own templates + system defaults (tenant_id=NULL)
        // Superadmin (currentTid==null) sees all
        if (currentTid != null) {
            wrapper.and(w -> w.isNull(SurveyTemplate::getTenantId)
                    .or().eq(SurveyTemplate::getTenantId, currentTid));
        }
        wrapper.orderByDesc(SurveyTemplate::getCreatedDate);
        var mpPage = templateMapper.selectPage(Page.of(page, size), wrapper);
        List<SurveyTemplateResponse> records = mpPage.getRecords().stream()
                .map(this::toTemplateResponse)
                .collect(Collectors.toList());
        return PageResponse.of(mpPage, records);
    }

    /**
     * Verify the current user can VIEW this template.
     * System defaults (tenant_id=NULL) are visible to all tenants.
     */
    private void checkTemplateTenantAccess(SurveyTemplate t) {
        if (t.getTenantId() == null) return; // system default — visible to all
        Long currentTid = SecurityUtils.getCurrentTenantIdOrNull();
        if (currentTid == null) return; // superadmin — sees all
        if (!currentTid.equals(t.getTenantId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * Verify the current user can access this instance (tenant isolation).
     * System instances (tenant_id=NULL) are visible to all tenants;
     * tenant-scoped instances are only visible to users within the same tenant.
     */
    private void checkInstanceTenantAccess(SurveyInstance instance) {
        if (instance.getTenantId() == null) return; // system — visible to all
        Long currentTid = SecurityUtils.getCurrentTenantIdOrNull();
        if (currentTid == null) return; // superadmin — sees all
        if (!currentTid.equals(instance.getTenantId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "cross-tenant instance access not allowed");
        }
    }

    /**
     * Verify the current user can MODIFY this template.
     * System defaults (tenant_id=NULL) can only be modified by superadmin.
     */
    private void checkTemplateTenantWriteAccess(SurveyTemplate t) {
        Long currentTid = SecurityUtils.getCurrentTenantIdOrNull();
        if (t.getTenantId() == null) {
            // System defaults — only superadmin can modify
            if (currentTid != null) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
            return;
        }
        if (currentTid == null) return; // superadmin
        if (!currentTid.equals(t.getTenantId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    @Override
    @Transactional
    public SurveyTemplateResponse updateTemplate(Long id, UpdateSurveyTemplateRequest request, Long adminId) {
        SurveyTemplate t = findTemplateOrFail(id);
        checkTemplateTenantAccess(t);
        if (StringUtils.hasText(request.getTitle())) t.setTitle(request.getTitle());
        if (request.getDescription() != null) t.setDescription(request.getDescription());

        // Handle status transitions
        if (request.getStatus() != null) {
            String newStatus = request.getStatus();
            if (BusinessConstants.SURVEY_STATUS_PUBLISHED.equals(newStatus)) {
                // Archive any previously published version in the same origin chain
                if (t.getOriginId() != null) {
                    templateMapper.selectList(new LambdaQueryWrapper<SurveyTemplate>()
                            .eq(SurveyTemplate::getOriginId, t.getOriginId())
                            .eq(SurveyTemplate::getStatus, BusinessConstants.SURVEY_STATUS_PUBLISHED))
                            .forEach(old -> {
                                old.setStatus(BusinessConstants.SURVEY_STATUS_ARCHIVED);
                                templateMapper.updateById(old);
                            });
                } else {
                    templateMapper.selectList(new LambdaQueryWrapper<SurveyTemplate>()
                            .eq(SurveyTemplate::getOriginId, t.getId())
                            .eq(SurveyTemplate::getStatus, BusinessConstants.SURVEY_STATUS_PUBLISHED))
                            .forEach(old -> {
                                old.setStatus(BusinessConstants.SURVEY_STATUS_ARCHIVED);
                                templateMapper.updateById(old);
                            });
                }
            }
            t.setStatus(newStatus);
        }

        t.setLastModifiedBy(adminId);
        t.setLastModifiedDate(System.currentTimeMillis());
        templateMapper.updateById(t);
        return toTemplateResponse(t);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        SurveyTemplate t = findTemplateOrFail(id);
        checkTemplateTenantWriteAccess(t);
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

        // Auto-create default first section
        SurveySection section = new SurveySection();
        section.setPageId(page.getId());
        section.setTitle("Section 1");
        section.setDisplayOrder(1);
        section.setCreatedBy(adminId);
        section.setCreatedDate(System.currentTimeMillis());
        sectionMapper.insert(section);

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

    @Override
    @Transactional
    public void updatePageTitle(Long pageId, String title) {
        SurveyPage page = pageMapper.selectById(pageId);
        if (page == null) throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND, "page not found");
        page.setTitle(title);
        pageMapper.updateById(page);
    }

    @Override
    @Transactional
    public void updateSectionTitle(Long sectionId, String title) {
        SurveySection section = sectionMapper.selectById(sectionId);
        if (section == null) throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND, "section not found");
        section.setTitle(title);
        sectionMapper.updateById(section);
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
    public void updateQuestionFields(Long questionId, Map<String, Object> fields, Long adminId) {
        SurveyQuestion q = questionMapper.selectById(questionId);
        if (q == null) throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND, "question not found");
        if (fields.containsKey("type")) q.setType((String) fields.get("type"));
        if (fields.containsKey("title")) q.setTitle((String) fields.get("title"));
        if (fields.containsKey("description")) q.setDescription((String) fields.get("description"));
        if (fields.containsKey("options")) q.setOptions((String) fields.get("options"));
        if (fields.containsKey("required")) q.setRequired(fields.get("required") instanceof Boolean b && b ? 1 : (Integer) fields.get("required"));
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
        rule.setRuleType(request.getRuleType() != null ? request.getRuleType() : "AND");
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

    // ── Instance Management ──

    @Override
    @Transactional
    public SurveyInstanceResponse createInstance(CreateSurveyInstanceRequest request, Long adminId) {
        SurveyTemplate template = findTemplateOrFail(request.getTemplateId());
        if (!BusinessConstants.SURVEY_STATUS_PUBLISHED.equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "only published templates can be distributed");
        }

        SurveyInstance instance = new SurveyInstance();
        instance.setTenantId(SecurityUtils.getCurrentTenantIdOrNull()); // null for superadmin
        instance.setTemplateId(template.getId());
        instance.setTitle(template.getTitle());
        instance.setStatus(BusinessConstants.INSTANCE_STATUS_READY);
        instance.setAssignedTo(request.getAssignedTo());
        instance.setTriggerType(request.getTriggerType() != null ? request.getTriggerType() : BusinessConstants.SURVEY_TRIGGER_MANUAL);
        instance.setTicketId(request.getTicketId());
        instance.setCreatedBy(adminId);
        instance.setCreatedDate(System.currentTimeMillis());
        instanceMapper.insert(instance);

        // Create instance pages for all template pages
        List<SurveyPage> pages = pageMapper.selectList(new LambdaQueryWrapper<SurveyPage>()
                .eq(SurveyPage::getTemplateId, template.getId()));
        Map<Long, Long> pageAssignees = request.getPageAssignees();
        for (SurveyPage page : pages) {
            SurveyInstancePage ip = new SurveyInstancePage();
            ip.setInstanceId(instance.getId());
            ip.setPageId(page.getId());
            ip.setStatus(BusinessConstants.INSTANCE_STATUS_READY);
            // Use per-page assignee if provided, otherwise inherit from instance
            if (pageAssignees != null && pageAssignees.containsKey(page.getId())) {
                ip.setAssignedTo(pageAssignees.get(page.getId()));
            } else {
                ip.setAssignedTo(request.getAssignedTo());
            }
            instancePageMapper.insert(ip);
        }

        log.info("Survey instance created: id={} templateId={} assignedTo={}", instance.getId(), template.getId(), request.getAssignedTo());
        return toInstanceResponse(instance);
    }

    @Override
    public List<SurveyInstanceResponse> listUserInstances(Long userId) {
        // Find instance IDs where user is a page assignee
        List<Long> pageInstanceIds = instancePageMapper.selectList(new LambdaQueryWrapper<SurveyInstancePage>()
                .eq(SurveyInstancePage::getAssignedTo, userId)
                .select(SurveyInstancePage::getInstanceId))
                .stream().map(SurveyInstancePage::getInstanceId).distinct().collect(Collectors.toList());

        LambdaQueryWrapper<SurveyInstance> wrapper = new LambdaQueryWrapper<>();
        // Tenant isolation (superadmin sees all)
        Long currentTid = SecurityUtils.getCurrentTenantIdOrNull();
        if (currentTid != null) {
            wrapper.and(w -> w.isNull(SurveyInstance::getTenantId)
                    .or().eq(SurveyInstance::getTenantId, currentTid));
        }
        wrapper.and(w -> w.eq(SurveyInstance::getAssignedTo, userId)
                .or().in(!pageInstanceIds.isEmpty(), SurveyInstance::getId, pageInstanceIds));
        wrapper.orderByDesc(SurveyInstance::getCreatedDate);
        return instanceMapper.selectList(wrapper)
                .stream().map(this::toInstanceResponse).collect(Collectors.toList());
    }

    @Override
    public List<SurveyInstanceResponse> listTemplateInstances(Long templateId) {
        // Verify template tenant access first, then inherit for instances
        SurveyTemplate template = findTemplateOrFail(templateId);
        checkTemplateTenantAccess(template);
        return instanceMapper.selectList(new LambdaQueryWrapper<SurveyInstance>()
                .eq(SurveyInstance::getTemplateId, templateId)
                .orderByDesc(SurveyInstance::getCreatedDate))
                .stream().map(this::toInstanceResponse).collect(Collectors.toList());
    }

    // ── Reassign ──

    @Override
    @Transactional
    public SurveyInstanceResponse reassignInstance(Long instanceId, ReassignRequest request, Long adminId) {
        SurveyInstance instance = findInstanceOrFail(instanceId);
        checkInstanceTenantAccess(instance);
        instance.setAssignedTo(request.getUserId());
        instanceMapper.updateById(instance);
        return toInstanceResponse(instance);
    }

    @Override
    @Transactional
    public void reassignPage(Long instanceId, Long pageId, ReassignRequest request, Long adminId) {
        SurveyInstance instance = findInstanceOrFail(instanceId);
        checkInstanceTenantAccess(instance);
        SurveyInstancePage ip = instancePageMapper.selectOne(new LambdaQueryWrapper<SurveyInstancePage>()
                .eq(SurveyInstancePage::getInstanceId, instanceId)
                .eq(SurveyInstancePage::getPageId, pageId));
        if (ip == null) {
            throw new BusinessException(ErrorCode.SURVEY_PAGE_NOT_FOUND);
        }
        ip.setAssignedTo(request.getUserId());
        instancePageMapper.updateById(ip);
    }

    @Override
    public PageResponse<UserResponse> listUsersForReassign(UserListRequest request) {
        int size = request.getSize() != null ? request.getSize() : 200;
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        // Only enabled users
        wrapper.eq(User::getStatus, 1);
        // Tenant isolation
        if (request.getTenantId() != null) {
            wrapper.eq(User::getTenantId, request.getTenantId());
        }
        wrapper.orderByAsc(User::getRole).orderByAsc(User::getUsername);
        IPage<User> result = userMapper.selectPage(Page.of(1, size), wrapper);
        return PageResponse.of(result, result.getRecords().stream().map(UserResponse::from).toList());
    }

    // ── Fill Flow ──

    @Override
    public SurveyFillResponse getFillData(Long instanceId, Long userId) {
        SurveyInstance instance = findInstanceOrFail(instanceId);
        checkInstanceTenantAccess(instance);
        // Verify access: instance assignee OR any page assignee can view
        if (!canAccessFillData(instanceId, instance, userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // Transition READY → IN_PROGRESS on first access
        if (BusinessConstants.INSTANCE_STATUS_READY.equals(instance.getStatus())) {
            instance.setStatus(BusinessConstants.INSTANCE_STATUS_IN_PROGRESS);
            instanceMapper.updateById(instance);
        }

        SurveyTemplate template = findTemplateOrFail(instance.getTemplateId());
        SurveyTemplateResponse templateResponse = toTemplateResponse(template);

        // Load existing answers
        Map<Long, String> existingAnswers = new HashMap<>();
        List<SurveyAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<SurveyAnswer>()
                .eq(SurveyAnswer::getInstanceId, instanceId));
        for (SurveyAnswer a : answers) {
            existingAnswers.put(a.getQuestionId(), a.getValue());
        }

        // Evaluate visibility
        Map<Long, Object> answerObjects = new HashMap<>(existingAnswers);
        Set<String> hidden = visibilityEngine.evaluateHidden(templateResponse, answerObjects);

        SurveyFillResponse response = new SurveyFillResponse();
        response.setInstanceId(instanceId);
        response.setInstanceStatus(instance.getStatus());
        response.setTitle(instance.getTitle());
        // Set current user's username
        User currentUser = userMapper.selectById(userId);
        response.setCurrentUsername(currentUser != null ? currentUser.getUsername() : null);
        response.setPages(templateResponse.getPages());
        response.setHiddenTargets(hidden);
        response.setExistingAnswers(existingAnswers);
        // Populate assignee name
        if (instance.getAssignedTo() != null) {
            User assignedUser = userMapper.selectById(instance.getAssignedTo());
            response.setAssignedToName(assignedUser != null ? assignedUser.getUsername() : null);
        }
        // Populate per-page statuses + assignees
        Map<Long, String> pageStatuses = new HashMap<>();
        Map<Long, String> pageAssignees = new HashMap<>();
        List<SurveyInstancePage> ipList = instancePageMapper.selectList(new LambdaQueryWrapper<SurveyInstancePage>()
                .eq(SurveyInstancePage::getInstanceId, instanceId));
        for (SurveyInstancePage ip : ipList) {
            pageStatuses.put(ip.getPageId(), ip.getStatus());
            if (ip.getAssignedTo() != null) {
                User pageAssignee = userMapper.selectById(ip.getAssignedTo());
                if (pageAssignee != null) {
                    pageAssignees.put(ip.getPageId(), pageAssignee.getUsername());
                }
            }
        }
        response.setPageStatuses(pageStatuses);
        response.setPageAssignees(pageAssignees);
        return response;
    }

    @Override
    @Transactional
    public void saveAnswer(Long instanceId, SaveAnswerRequest request, Long userId) {
        SurveyInstance instance = findInstanceOrFail(instanceId);
        checkInstanceTenantAccess(instance);
        // Page-level assignee check takes precedence, fall back to instance-level
        if (!hasPageOrInstancePermission(instanceId, instance, request.getQuestionId(), userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (BusinessConstants.INSTANCE_STATUS_SUBMITTED.equals(instance.getStatus())
                || BusinessConstants.INSTANCE_STATUS_COMPLETED.equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.INSTANCE_ALREADY_SUBMITTED);
        }

        upsertAnswer(instanceId, request);
        updateInstancePageStatus(instanceId, request.getQuestionId());
    }

    /**
     * Check permission to answer a question: page assignee first, then instance assignee.
     */
    private boolean hasPageOrInstancePermission(Long instanceId, SurveyInstance instance, Long questionId, Long userId) {
        Long pageAssignTo = resolvePageAssignee(instanceId, questionId);
        if (pageAssignTo != null) {
            return pageAssignTo.equals(userId);
        }
        // No page-level assignee — fall back to instance-level
        return instance.getAssignedTo() != null && instance.getAssignedTo().equals(userId);
    }

    /**
     * Resolve the page-level assignee for a question.
     * Returns null if the page has no explicit assignee.
     */
    private Long resolvePageAssignee(Long instanceId, Long questionId) {
        SurveyQuestion question = questionMapper.selectById(questionId);
        if (question == null) return null;
        SurveySection section = sectionMapper.selectById(question.getSectionId());
        if (section == null) return null;
        SurveyInstancePage ip = instancePageMapper.selectOne(new LambdaQueryWrapper<SurveyInstancePage>()
                .eq(SurveyInstancePage::getInstanceId, instanceId)
                .eq(SurveyInstancePage::getPageId, section.getPageId()));
        return ip != null ? ip.getAssignedTo() : null;
    }

    /** Upsert a single answer (no permission check — caller validates access). */
    private void upsertAnswer(Long instanceId, SaveAnswerRequest request) {
        SurveyAnswer existing = answerMapper.selectOne(new LambdaQueryWrapper<SurveyAnswer>()
                .eq(SurveyAnswer::getInstanceId, instanceId)
                .eq(SurveyAnswer::getQuestionId, request.getQuestionId()));
        if (existing != null) {
            existing.setValue(request.getValue());
            answerMapper.updateById(existing);
        } else {
            SurveyAnswer answer = new SurveyAnswer();
            answer.setInstanceId(instanceId);
            answer.setQuestionId(request.getQuestionId());
            answer.setValue(request.getValue());
            answerMapper.insert(answer);
        }
    }

    /** Update instance page status to IN_PROGRESS (non-critical, errors logged but not thrown). */
    private void updateInstancePageStatus(Long instanceId, Long questionId) {
        try {
            SurveyQuestion question = questionMapper.selectById(questionId);
            if (question != null) {
                SurveySection section = sectionMapper.selectById(question.getSectionId());
                if (section != null) {
                    SurveyPage page = pageMapper.selectById(section.getPageId());
                    if (page != null) {
                        SurveyInstancePage ip = instancePageMapper.selectOne(new LambdaQueryWrapper<SurveyInstancePage>()
                                .eq(SurveyInstancePage::getInstanceId, instanceId)
                                .eq(SurveyInstancePage::getPageId, page.getId()));
                        if (ip != null && !BusinessConstants.INSTANCE_STATUS_COMPLETED.equals(ip.getStatus())) {
                            ip.setStatus(BusinessConstants.INSTANCE_STATUS_IN_PROGRESS);
                            instancePageMapper.updateById(ip);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update instance page status: {} (answer saved successfully)", e.getMessage());
        }
    }

    @Override
    @Transactional
    public SurveyInstanceResponse submitSurvey(Long instanceId, SubmitSurveyRequest request, Long userId) {
        SurveyInstance instance = findInstanceOrFail(instanceId);
        checkInstanceTenantAccess(instance);
        if (instance.getAssignedTo() == null || !instance.getAssignedTo().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (BusinessConstants.INSTANCE_STATUS_SUBMITTED.equals(instance.getStatus())
                || BusinessConstants.INSTANCE_STATUS_COMPLETED.equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.INSTANCE_ALREADY_SUBMITTED);
        }

        // Save any final answers from the submit request (skip per-answer page check — already validated at instance level)
        if (request.getAnswers() != null) {
            for (SaveAnswerRequest ans : request.getAnswers()) {
                upsertAnswer(instanceId, ans);
                updateInstancePageStatus(instanceId, ans.getQuestionId());
            }
        }

        // Validate all required questions across all pages
        SurveyTemplate template = findTemplateOrFail(instance.getTemplateId());
        SurveyTemplateResponse templateResponse = toTemplateResponse(template);

        // Load existing answers
        Map<Long, String> existingAnswers = new HashMap<>();
        List<SurveyAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<SurveyAnswer>()
                .eq(SurveyAnswer::getInstanceId, instanceId));
        for (SurveyAnswer a : answers) {
            existingAnswers.put(a.getQuestionId(), a.getValue());
        }

        // Check required questions are answered
        Map<Long, Object> answerObjects = new HashMap<>(existingAnswers);
        Set<String> hidden = visibilityEngine.evaluateHidden(templateResponse, answerObjects);

        for (SurveyTemplateResponse.PageResponse page : templateResponse.getPages()) {
            if (hidden.contains("PAGE:" + page.getId())) continue;
            for (SurveyTemplateResponse.SectionResponse section : page.getSections()) {
                if (hidden.contains("SECTION:" + section.getId())) continue;
                for (SurveyTemplateResponse.QuestionResponse q : section.getQuestions()) {
                    if (hidden.contains("QUESTION:" + q.getId())) continue;
                    if (Boolean.TRUE.equals(q.getRequired())) {
                        String val = existingAnswers.get(q.getId());
                        if (val == null || val.isEmpty() || "null".equals(val)) {
                            throw new BusinessException(ErrorCode.SURVEY_PAGE_INCOMPLETE,
                                    "Question \"" + q.getTitle() + "\" is required");
                        }
                    }
                }
            }
        }

        // Mark all instance pages as complete
        List<SurveyInstancePage> ipList = instancePageMapper.selectList(new LambdaQueryWrapper<SurveyInstancePage>()
                .eq(SurveyInstancePage::getInstanceId, instanceId));
        for (SurveyInstancePage ip : ipList) {
            ip.setStatus(BusinessConstants.INSTANCE_STATUS_SUBMITTED);
            instancePageMapper.updateById(ip);
        }

        instance.setStatus(BusinessConstants.INSTANCE_STATUS_SUBMITTED);
        instanceMapper.updateById(instance);

        log.info("Survey submitted: instanceId={} userId={}", instanceId, userId);
        return toInstanceResponse(instance);
    }

    // ── Results ──

    @Override
    public SurveyResultResponse getTemplateResults(Long templateId) {
        SurveyTemplate template = findTemplateOrFail(templateId);
        SurveyResultResponse result = new SurveyResultResponse();
        result.setTemplateId(templateId);
        result.setTemplateTitle(template.getTitle());

        List<SurveyInstance> instances = instanceMapper.selectList(new LambdaQueryWrapper<SurveyInstance>()
                .eq(SurveyInstance::getTemplateId, templateId));
        result.setTotalInstances(instances.size());
        result.setCompletedInstances((int) instances.stream()
                .filter(i -> BusinessConstants.INSTANCE_STATUS_SUBMITTED.equals(i.getStatus())
                        || BusinessConstants.INSTANCE_STATUS_COMPLETED.equals(i.getStatus()))
                .count());

        // Aggregate answers by question
        List<SurveyQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<SurveyQuestion>()
                .inSql(SurveyQuestion::getSectionId,
                        "SELECT id FROM survey_section WHERE page_id IN (SELECT id FROM survey_page WHERE template_id = "
                                + templateId + ")")
                .orderByAsc(SurveyQuestion::getDisplayOrder));

        List<SurveyResultResponse.QuestionResult> qResults = new ArrayList<>();
        for (SurveyQuestion q : questions) {
            SurveyResultResponse.QuestionResult qr = new SurveyResultResponse.QuestionResult();
            qr.setQuestionId(q.getId());
            qr.setTitle(q.getTitle());
            qr.setType(q.getType());

            List<SurveyAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<SurveyAnswer>()
                    .eq(SurveyAnswer::getQuestionId, q.getId()));

            if ("SINGLE_CHOICE".equals(q.getType()) || "DROPDOWN".equals(q.getType())
                    || "MULTI_CHOICE".equals(q.getType())) {
                Map<String, Integer> counts = new HashMap<>();
                for (SurveyAnswer a : answers) {
                    String[] vals = a.getValue() != null ? a.getValue().split(",") : new String[0];
                    for (String v : vals) {
                        String trimmed = v.trim();
                        if (!trimmed.isEmpty())
                            counts.merge(trimmed, 1, Integer::sum);
                    }
                }
                qr.setChoiceCounts(counts);
            } else {
                List<String> texts = answers.stream()
                        .map(SurveyAnswer::getValue)
                        .filter(v -> v != null && !v.isEmpty())
                        .collect(Collectors.toList());
                qr.setTextAnswers(texts);
            }
            qResults.add(qr);
        }
        result.setQuestions(qResults);
        return result;
    }

    // ── Private Helpers ──

    private SurveyInstance findInstanceOrFail(Long id) {
        SurveyInstance instance = instanceMapper.selectById(id);
        if (instance == null) throw new BusinessException(ErrorCode.INSTANCE_NOT_FOUND);
        return instance;
    }

    /**
     * Check if user can access fill data: instance assignee or any page assignee.
     */
    private boolean canAccessFillData(Long instanceId, SurveyInstance instance, Long userId) {
        // Instance assignee always has access
        if (instance.getAssignedTo() != null && instance.getAssignedTo().equals(userId)) {
            return true;
        }
        // Check if user is assigned to any page of this instance
        List<SurveyInstancePage> pages = instancePageMapper.selectList(new LambdaQueryWrapper<SurveyInstancePage>()
                .eq(SurveyInstancePage::getInstanceId, instanceId));
        return pages.stream().anyMatch(ip -> ip.getAssignedTo() != null && ip.getAssignedTo().equals(userId));
    }

    private SurveyInstanceResponse toInstanceResponse(SurveyInstance instance) {
        SurveyInstanceResponse r = new SurveyInstanceResponse();
        r.setId(instance.getId());
        r.setTenantId(instance.getTenantId());
        r.setTitle(instance.getTitle());
        r.setStatus(instance.getStatus());
        r.setAssignedTo(instance.getAssignedTo());
        if (instance.getAssignedTo() != null) {
            User assignedUser = userMapper.selectById(instance.getAssignedTo());
            r.setAssignedToName(assignedUser != null ? assignedUser.getUsername() : null);
        }
        r.setTriggerType(instance.getTriggerType());
        r.setTicketId(instance.getTicketId());
        r.setCreatedDate(instance.getCreatedDate());

        // Lookup template info
        SurveyTemplate template = templateMapper.selectById(instance.getTemplateId());
        if (template != null) {
            r.setTemplateTitle(template.getTitle());
            Long pageCount = pageMapper.selectCount(new LambdaQueryWrapper<SurveyPage>()
                    .eq(SurveyPage::getTemplateId, template.getId()));
            r.setTotalPages(pageCount.intValue());
        }

        // Count completed pages
        Long completedCount = instancePageMapper.selectCount(new LambdaQueryWrapper<SurveyInstancePage>()
                .eq(SurveyInstancePage::getInstanceId, instance.getId())
                .eq(SurveyInstancePage::getStatus, BusinessConstants.INSTANCE_STATUS_SUBMITTED));
        r.setCompletedPages(completedCount.intValue());

        return r;
    }

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
                        .orderByAsc(SurveyVisibilityRule::getRuleType, SurveyVisibilityRule::getDisplayOrder));
        return rules.stream().map(this::toRuleResponse).collect(Collectors.toList());
    }

    private SurveyTemplateResponse.VisibilityRuleResponse toRuleResponse(SurveyVisibilityRule rl) {
        SurveyTemplateResponse.VisibilityRuleResponse vr = new SurveyTemplateResponse.VisibilityRuleResponse();
        vr.setId(rl.getId());
        vr.setSourceQuestionId(rl.getSourceQuestionId());
        vr.setOp(rl.getOp());
        vr.setValue(rl.getValue());
        vr.setRuleType(rl.getRuleType() != null ? rl.getRuleType() : "AND");
        return vr;
    }
}
