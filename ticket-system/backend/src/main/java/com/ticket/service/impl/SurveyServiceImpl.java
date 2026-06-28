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
    private final SurveyInstanceLogMapper instanceLogMapper;
    private final SurveyVisibilityEngine visibilityEngine;
    private final UserMapper userMapper;

    // Instance log action constants
    private static final String LOG_INSTANCE_CREATED = "INSTANCE_CREATED";
    private static final String LOG_ANSWER_SAVED = "ANSWER_SAVED";
    private static final String LOG_PAGE_COMPLETED = "PAGE_COMPLETED";
    private static final String LOG_PAGE_REOPENED = "PAGE_REOPENED";
    private static final String LOG_INSTANCE_REOPENED = "INSTANCE_REOPENED";
    private static final String LOG_INSTANCE_SUBMITTED = "INSTANCE_SUBMITTED";
    private static final String LOG_INSTANCE_REASSIGNED = "INSTANCE_REASSIGNED";

    public SurveyServiceImpl(SurveyTemplateMapper templateMapper, SurveyPageMapper pageMapper,
                             SurveySectionMapper sectionMapper, SurveyQuestionMapper questionMapper,
                             SurveyVisibilityRuleMapper ruleMapper, SurveyInstanceMapper instanceMapper,
                             SurveyInstancePageMapper instancePageMapper, SurveyAnswerMapper answerMapper,
                             SurveyInstanceLogMapper instanceLogMapper,
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
        this.instanceLogMapper = instanceLogMapper;
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

        // Create a default first section (so "add question" is available immediately)
        SurveySection section = new SurveySection();
        section.setPageId(page.getId());
        section.setTitle("Section 1");
        section.setDisplayOrder(1);
        section.setCreatedBy(adminId);
        section.setCreatedDate(System.currentTimeMillis());
        sectionMapper.insert(section);

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
                // Atomically archive any previously published version in the same origin chain
                // Single UPDATE prevents race conditions between concurrent publish operations
                Long originId = t.getOriginId() != null ? t.getOriginId() : t.getId();
                com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SurveyTemplate> archiveWrapper =
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
                archiveWrapper.set(SurveyTemplate::getStatus, BusinessConstants.SURVEY_STATUS_ARCHIVED)
                        .eq(SurveyTemplate::getOriginId, originId)
                        .eq(SurveyTemplate::getStatus, BusinessConstants.SURVEY_STATUS_PUBLISHED);
                templateMapper.update(null, archiveWrapper);
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
        List<Long> missing = new ArrayList<>();
        for (ReorderRequest.ReorderItem item : request.getItems()) {
            SurveyPage page = pageMapper.selectById(item.getId());
            if (page == null || !page.getTemplateId().equals(templateId)) {
                missing.add(item.getId());
            }
        }
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND,
                    "pages not found: " + missing);
        }
        for (ReorderRequest.ReorderItem item : request.getItems()) {
            SurveyPage page = pageMapper.selectById(item.getId());
            page.setDisplayOrder(item.getDisplayOrder());
            pageMapper.updateById(page);
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
        if (page == null) throw new BusinessException(ErrorCode.PAGE_NOT_FOUND);
        page.setTitle(title);
        pageMapper.updateById(page);
    }

    @Override
    @Transactional
    public void updateSectionTitle(Long sectionId, String title) {
        SurveySection section = sectionMapper.selectById(sectionId);
        if (section == null) throw new BusinessException(ErrorCode.SECTION_NOT_FOUND);
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
        if (q == null) throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
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
        if (q == null) throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        if (fields.containsKey("type")) q.setType((String) fields.get("type"));
        if (fields.containsKey("title")) q.setTitle((String) fields.get("title"));
        if (fields.containsKey("description")) q.setDescription((String) fields.get("description"));
        if (fields.containsKey("options")) q.setOptions((String) fields.get("options"));
        if (fields.containsKey("required")) {
            Object reqVal = fields.get("required");
            if (reqVal instanceof Boolean b) {
                q.setRequired(b ? 1 : 0);
            } else if (reqVal instanceof Number n) {
                q.setRequired(n.intValue() != 0 ? 1 : 0);
            } else if (reqVal instanceof String s) {
                q.setRequired("true".equalsIgnoreCase(s) || "1".equals(s) ? 1 : 0);
            }
        }
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
        List<Long> missing = new ArrayList<>();
        for (ReorderRequest.ReorderItem item : request.getItems()) {
            SurveyQuestion q = questionMapper.selectById(item.getId());
            if (q == null || !q.getSectionId().equals(sectionId)) {
                missing.add(item.getId());
            }
        }
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND,
                    "questions not found: " + missing);
        }
        for (ReorderRequest.ReorderItem item : request.getItems()) {
            SurveyQuestion q = questionMapper.selectById(item.getId());
            q.setDisplayOrder(item.getDisplayOrder());
            questionMapper.updateById(q);
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
        logActivity(instance.getId(), null, null, LOG_INSTANCE_CREATED, adminId, "Assigned to userId=" + request.getAssignedTo());
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
        logActivity(instanceId, null, null, LOG_INSTANCE_REASSIGNED, adminId, "Reassigned to userId=" + request.getUserId());
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
    @Transactional
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

        String oldVal = upsertAnswer(instanceId, request);
        String newVal = request.getValue();
        String displayOld = resolveLabels(request.getQuestionId(), oldVal);
        String displayNew = resolveLabels(request.getQuestionId(), newVal);
        String detail;
        if (oldVal == null) {
            detail = "Initial answer: " + displayNew;
        } else if (!newVal.equals(oldVal)) {
            detail = "Value changed: " + displayOld + " → " + displayNew;
        } else {
            detail = "Answer unchanged (" + displayNew + ")";
        }
        logActivity(instanceId, null, request.getQuestionId(), LOG_ANSWER_SAVED, userId, detail);
        updateInstancePageStatus(instanceId, request.getQuestionId(), instance);

        // Re-check status after write to prevent TOCTOU: concurrent submit between the
        // initial status guard and this upsert could have committed without us seeing it.
        SurveyInstance fresh = instanceMapper.selectById(instanceId);
        if (fresh != null && (BusinessConstants.INSTANCE_STATUS_SUBMITTED.equals(fresh.getStatus())
                || BusinessConstants.INSTANCE_STATUS_COMPLETED.equals(fresh.getStatus()))) {
            throw new BusinessException(ErrorCode.INSTANCE_ALREADY_SUBMITTED,
                    "survey was submitted while saving, please refresh");
        }
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

    /**
     * Upsert a single answer (no permission check — caller validates access).
     * @return the old value before upsert, or null if this is a new answer
     */
    private String upsertAnswer(Long instanceId, SaveAnswerRequest request) {
        SurveyAnswer existing = answerMapper.selectOne(new LambdaQueryWrapper<SurveyAnswer>()
                .eq(SurveyAnswer::getInstanceId, instanceId)
                .eq(SurveyAnswer::getQuestionId, request.getQuestionId()));
        String oldValue = existing != null ? existing.getValue() : null;
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
        return oldValue;
    }

    /** Update instance page status to IN_PROGRESS (non-critical, transient errors logged but not thrown). */
    private void updateInstancePageStatus(Long instanceId, Long questionId, SurveyInstance instance) {
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
                        // REOPEN is treated as editable — keep the status until completed
                        if (ip != null && !BusinessConstants.INSTANCE_STATUS_COMPLETED.equals(ip.getStatus())
                                && !BusinessConstants.INSTANCE_STATUS_REOPEN.equals(ip.getStatus())) {
                            ip.setStatus(BusinessConstants.INSTANCE_STATUS_IN_PROGRESS);
                            instancePageMapper.updateById(ip);
                        }
                    }
                }
            }
        } catch (org.springframework.dao.DataAccessException e) {
            // Transient DB issue — log but don't fail the answer save
            log.warn("Failed to update instance page status (transient DB error): {}", e.getMessage());
        }
        // Hard errors (RuntimeException, NPE, etc.) propagate — they indicate a bug, not a transient issue
    }

    @Override
    @Transactional
    public SurveyInstanceResponse completePage(Long instanceId, Long pageId, Long userId) {
        SurveyInstance instance = findInstanceOrFail(instanceId);
        checkInstanceTenantAccess(instance);
        if (!canAccessFillData(instanceId, instance, userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!BusinessConstants.INSTANCE_STATUS_READY.equals(instance.getStatus())
                && !BusinessConstants.INSTANCE_STATUS_IN_PROGRESS.equals(instance.getStatus())
                && !BusinessConstants.INSTANCE_STATUS_REOPEN.equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.INSTANCE_ALREADY_SUBMITTED);
        }

        // Mark page as completed
        SurveyInstancePage ip = instancePageMapper.selectOne(new LambdaQueryWrapper<SurveyInstancePage>()
                .eq(SurveyInstancePage::getInstanceId, instanceId)
                .eq(SurveyInstancePage::getPageId, pageId));
        if (ip == null) {
            throw new BusinessException(ErrorCode.SURVEY_PAGE_NOT_FOUND);
        }
        ip.setStatus(BusinessConstants.INSTANCE_STATUS_COMPLETED);
        instancePageMapper.updateById(ip);

        // Transition instance to IN_PROGRESS if needed
        if (BusinessConstants.INSTANCE_STATUS_READY.equals(instance.getStatus())) {
            instance.setStatus(BusinessConstants.INSTANCE_STATUS_IN_PROGRESS);
            instanceMapper.updateById(instance);
        }

        // If all pages are now completed, auto-transition instance to SUBMITTED
        if (allPagesCompleted(instance)) {
            instance.setStatus(BusinessConstants.INSTANCE_STATUS_SUBMITTED);
            instanceMapper.updateById(instance);
            log.info("All pages completed — instance auto-submitted: instanceId={}", instanceId);
            logActivity(instanceId, null, null, LOG_INSTANCE_SUBMITTED, userId, "Auto-submitted after all pages completed");
        }

        log.info("Page completed: instanceId={} pageId={} userId={}", instanceId, pageId, userId);
        logActivity(instanceId, pageId, null, LOG_PAGE_COMPLETED, userId, null);
        return toInstanceResponse(instance);
    }

    @Override
    @Transactional
    public SurveyInstanceResponse reopenPage(Long instanceId, Long pageId, Long userId) {
        SurveyInstance instance = findInstanceOrFail(instanceId);
        checkInstanceTenantAccess(instance);
        if (!canAccessFillData(instanceId, instance, userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        // Allow reopen from COMPLETED, SUBMITTED, or IN_PROGRESS (but not READY — nothing to reopen)
        if (BusinessConstants.INSTANCE_STATUS_READY.equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.SURVEY_PAGE_INCOMPLETE, "instance is not started yet");
        }

        SurveyInstancePage ip = instancePageMapper.selectOne(new LambdaQueryWrapper<SurveyInstancePage>()
                .eq(SurveyInstancePage::getInstanceId, instanceId)
                .eq(SurveyInstancePage::getPageId, pageId));
        if (ip == null) {
            throw new BusinessException(ErrorCode.SURVEY_PAGE_NOT_FOUND);
        }

        ip.setStatus(BusinessConstants.INSTANCE_STATUS_REOPEN);
        instancePageMapper.updateById(ip);

        // Roll back instance status so it can be worked on again
        if (BusinessConstants.INSTANCE_STATUS_SUBMITTED.equals(instance.getStatus())
                || BusinessConstants.INSTANCE_STATUS_COMPLETED.equals(instance.getStatus())) {
            instance.setStatus(BusinessConstants.INSTANCE_STATUS_REOPEN);
            instanceMapper.updateById(instance);
        }

        log.info("Page reopened: instanceId={} pageId={} userId={}", instanceId, pageId, userId);
        logActivity(instanceId, pageId, null, LOG_PAGE_REOPENED, userId, null);
        return toInstanceResponse(instance);
    }

    @Override
    @Transactional
    public SurveyInstanceResponse reopenInstance(Long instanceId, Long userId) {
        SurveyInstance instance = findInstanceOrFail(instanceId);
        checkInstanceTenantAccess(instance);
        if (!canAccessFillData(instanceId, instance, userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!BusinessConstants.INSTANCE_STATUS_SUBMITTED.equals(instance.getStatus())
                && !BusinessConstants.INSTANCE_STATUS_COMPLETED.equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.SURVEY_PAGE_INCOMPLETE,
                    "instance is not submitted or completed");
        }

        // Reopen all pages
        List<SurveyInstancePage> ipList = instancePageMapper.selectList(new LambdaQueryWrapper<SurveyInstancePage>()
                .eq(SurveyInstancePage::getInstanceId, instanceId));
        for (SurveyInstancePage ip : ipList) {
            ip.setStatus(BusinessConstants.INSTANCE_STATUS_REOPEN);
            instancePageMapper.updateById(ip);
        }

        instance.setStatus(BusinessConstants.INSTANCE_STATUS_REOPEN);
        instanceMapper.updateById(instance);

        log.info("Instance reopened: instanceId={} userId={}", instanceId, userId);
        logActivity(instanceId, null, null, LOG_INSTANCE_REOPENED, userId, null);
        return toInstanceResponse(instance);
    }

    @Override
    @Transactional
    public SurveyInstanceResponse submitSurvey(Long instanceId, SubmitSurveyRequest request, Long userId) {
        SurveyInstance instance = findInstanceOrFail(instanceId);
        checkInstanceTenantAccess(instance);
        if (!canAccessFillData(instanceId, instance, userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (BusinessConstants.INSTANCE_STATUS_COMPLETED.equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.INSTANCE_ALREADY_SUBMITTED);
        }
        if (!BusinessConstants.INSTANCE_STATUS_SUBMITTED.equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.SURVEY_PAGE_INCOMPLETE,
                    "All pages must be completed before completing the instance");
        }

        instance.setStatus(BusinessConstants.INSTANCE_STATUS_COMPLETED);
        instanceMapper.updateById(instance);

        log.info("Survey completed: instanceId={} userId={}", instanceId, userId);
        logActivity(instanceId, null, null, LOG_INSTANCE_SUBMITTED, userId, null);
        return toInstanceResponse(instance);
    }

    /** Check if all visible (non-hidden) pages are completed. */
    private boolean allPagesCompleted(SurveyInstance instance) {
        List<SurveyInstancePage> ipList = instancePageMapper.selectList(new LambdaQueryWrapper<SurveyInstancePage>()
                .eq(SurveyInstancePage::getInstanceId, instance.getId()));
        for (SurveyInstancePage ip : ipList) {
            if (!BusinessConstants.INSTANCE_STATUS_COMPLETED.equals(ip.getStatus())) {
                return false;
            }
        }
        return !ipList.isEmpty();
    }

    @Override
    public List<SurveyInstanceLogResponse> getInstanceLog(Long instanceId) {
        List<SurveyInstanceLog> entries = instanceLogMapper.selectList(new LambdaQueryWrapper<SurveyInstanceLog>()
                .eq(SurveyInstanceLog::getInstanceId, instanceId)
                .orderByDesc(SurveyInstanceLog::getCreatedDate));
        return entries.stream().map(SurveyInstanceLogResponse::from).collect(Collectors.toList());
    }

    // ── Clone ──

    @Override
    @Transactional
    public SurveyTemplateResponse cloneTemplate(Long templateId, Long adminId) {
        SurveyTemplate source = findTemplateOrFail(templateId);
        checkTemplateTenantAccess(source);

        // Create new template
        SurveyTemplate clone = new SurveyTemplate();
        clone.setTenantId(SecurityUtils.getCurrentTenantIdOrNull());
        clone.setTitle("Copy of " + source.getTitle());
        clone.setDescription(source.getDescription());
        clone.setStatus(BusinessConstants.SURVEY_STATUS_DRAFT);
        clone.setVersion(1);
        clone.setAllowResubmit(source.getAllowResubmit());
        clone.setCreatedBy(adminId);
        clone.setCreatedDate(System.currentTimeMillis());
        templateMapper.insert(clone);

        // Clone pages → sections → questions
        List<SurveyPage> pages = pageMapper.selectList(
                new LambdaQueryWrapper<SurveyPage>()
                        .eq(SurveyPage::getTemplateId, templateId)
                        .orderByAsc(SurveyPage::getDisplayOrder));

        Map<Long, Long> pageIdMap = new HashMap<>();
        Map<Long, Long> sectionIdMap = new HashMap<>();
        Map<Long, Long> questionIdMap = new HashMap<>();

        for (SurveyPage page : pages) {
            Long oldPageId = page.getId();
            page.setId(null);
            page.setTemplateId(clone.getId());
            page.setCreatedBy(adminId);
            page.setCreatedDate(System.currentTimeMillis());
            page.setLastModifiedBy(null);
            page.setLastModifiedDate(null);
            pageMapper.insert(page);
            pageIdMap.put(oldPageId, page.getId());

            // Clone sections
            List<SurveySection> sections = sectionMapper.selectList(
                    new LambdaQueryWrapper<SurveySection>()
                            .eq(SurveySection::getPageId, oldPageId)
                            .orderByAsc(SurveySection::getDisplayOrder));

            for (SurveySection section : sections) {
                Long oldSectionId = section.getId();
                section.setId(null);
                section.setPageId(page.getId());
                section.setCreatedBy(adminId);
                section.setCreatedDate(System.currentTimeMillis());
                section.setLastModifiedBy(null);
                section.setLastModifiedDate(null);
                sectionMapper.insert(section);
                sectionIdMap.put(oldSectionId, section.getId());

                // Clone questions
                List<SurveyQuestion> questions = questionMapper.selectList(
                        new LambdaQueryWrapper<SurveyQuestion>()
                                .eq(SurveyQuestion::getSectionId, oldSectionId)
                                .orderByAsc(SurveyQuestion::getDisplayOrder));

                for (SurveyQuestion question : questions) {
                    Long oldQuestionId = question.getId();
                    question.setId(null);
                    question.setSectionId(section.getId());
                    question.setCreatedBy(adminId);
                    question.setCreatedDate(System.currentTimeMillis());
                    question.setLastModifiedBy(null);
                    question.setLastModifiedDate(null);
                    questionMapper.insert(question);
                    questionIdMap.put(oldQuestionId, question.getId());
                }
            }
        }

        // Clone visibility rules with remapped IDs
        List<SurveyVisibilityRule> rules = ruleMapper.selectList(
                new LambdaQueryWrapper<SurveyVisibilityRule>()
                        .eq(SurveyVisibilityRule::getTemplateId, templateId));

        for (SurveyVisibilityRule rule : rules) {
            rule.setId(null);
            rule.setTemplateId(clone.getId());
            rule.setCreatedBy(adminId);
            rule.setCreatedDate(System.currentTimeMillis());
            rule.setLastModifiedBy(null);
            rule.setLastModifiedDate(null);

            // Remap target IDs based on target type
            if ("PAGE".equalsIgnoreCase(rule.getTargetType()) && rule.getTargetId() != null
                    && pageIdMap.containsKey(rule.getTargetId())) {
                rule.setTargetId(pageIdMap.get(rule.getTargetId()));
            } else if ("SECTION".equalsIgnoreCase(rule.getTargetType()) && rule.getTargetId() != null
                    && sectionIdMap.containsKey(rule.getTargetId())) {
                rule.setTargetId(sectionIdMap.get(rule.getTargetId()));
            } else if ("QUESTION".equalsIgnoreCase(rule.getTargetType()) && rule.getTargetId() != null
                    && questionIdMap.containsKey(rule.getTargetId())) {
                rule.setTargetId(questionIdMap.get(rule.getTargetId()));
            }
            // Remap source question ID
            if (rule.getSourceQuestionId() != null && questionIdMap.containsKey(rule.getSourceQuestionId())) {
                rule.setSourceQuestionId(questionIdMap.get(rule.getSourceQuestionId()));
            }

            ruleMapper.insert(rule);
        }

        log.info("Survey template cloned: sourceId={} cloneId={} title={}", templateId, clone.getId(), clone.getTitle());
        return toTemplateResponse(clone);
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

    // ── Helpers ──

    /** Resolve option keys like "opt_1,opt_2" to labels like "Production, Staging" */
    private String resolveLabels(Long questionId, String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) return "\"\"";
        try {
            SurveyQuestion q = questionMapper.selectById(questionId);
            if (q == null) {
                log.warn("resolveLabels: question not found id={}", questionId);
                return "\"" + rawValue + "\"";
            }
            if (q.getOptions() == null || q.getOptions().isEmpty()) {
                log.debug("resolveLabels: question {} has no options", questionId);
                return "\"" + rawValue + "\"";
            }
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            var root = om.readTree(q.getOptions());
            // Handle both formats: raw array [{"key":...,"label":...}]
            // and wrapped {"options": [{"key":...,"label":...}]}
            var nodes = root.isArray() ? root : (root.has("options") ? root.get("options") : root);
            Map<String, String> keyToLabel = new java.util.LinkedHashMap<>();
            for (var node : nodes) {
                if (node.has("key") && node.has("label")) {
                    keyToLabel.put(node.get("key").asText(), node.get("label").asText());
                }
            }
            if (keyToLabel.isEmpty()) {
                log.debug("resolveLabels: no key→label mappings, options={}", q.getOptions());
                return "\"" + rawValue + "\"";
            }
            String[] keys = rawValue.split(",");
            String[] labels = java.util.Arrays.stream(keys)
                    .map(String::trim)
                    .map(k -> keyToLabel.getOrDefault(k, k))
                    .toArray(String[]::new);
            return "\"" + String.join(", ", labels) + "\"";
        } catch (Exception e) {
            log.error("resolveLabels failed for questionId={}: {}", questionId, e.getMessage(), e);
            return "\"" + rawValue + "\"";
        }
    }

    // ── Instance Activity Log ──

    private void logActivity(Long instanceId, Long pageId, Long questionId, String action, Long userId, String detail) {
        try {
            SurveyInstanceLog entry = new SurveyInstanceLog();
            entry.setInstanceId(instanceId);
            entry.setPageId(pageId);
            entry.setQuestionId(questionId);
            entry.setAction(action);
            entry.setUserId(userId);
            entry.setDetail(detail);
            entry.setCreatedDate(System.currentTimeMillis());
            instanceLogMapper.insert(entry);
        } catch (Exception e) {
            log.error("Failed to write instance activity log: {}", e.getMessage(), e);
        }
    }
}
