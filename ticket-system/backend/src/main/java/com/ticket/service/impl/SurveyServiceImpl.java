package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    public SurveyServiceImpl(SurveyTemplateMapper templateMapper, SurveyPageMapper pageMapper,
                             SurveySectionMapper sectionMapper, SurveyQuestionMapper questionMapper,
                             SurveyVisibilityRuleMapper ruleMapper, SurveyInstanceMapper instanceMapper,
                             SurveyInstancePageMapper instancePageMapper, SurveyAnswerMapper answerMapper,
                             SurveyVisibilityEngine visibilityEngine) {
        this.templateMapper = templateMapper;
        this.pageMapper = pageMapper;
        this.sectionMapper = sectionMapper;
        this.questionMapper = questionMapper;
        this.ruleMapper = ruleMapper;
        this.instanceMapper = instanceMapper;
        this.instancePageMapper = instancePageMapper;
        this.answerMapper = answerMapper;
        this.visibilityEngine = visibilityEngine;
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
    public PageResponse<SurveyTemplateResponse> listTemplates(int page, int size) {
        var mpPage = templateMapper.selectPage(
                Page.of(page, size),
                new LambdaQueryWrapper<SurveyTemplate>()
                        .orderByDesc(SurveyTemplate::getCreatedDate));
        List<SurveyTemplateResponse> records = mpPage.getRecords().stream()
                .map(this::toTemplateResponse)
                .collect(Collectors.toList());
        return PageResponse.of(mpPage, records);
    }

    @Override
    @Transactional
    public SurveyTemplateResponse updateTemplate(Long id, UpdateSurveyTemplateRequest request, Long adminId) {
        SurveyTemplate t = findTemplateOrFail(id);
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

    // ── Instance Management ──

    @Override
    @Transactional
    public SurveyInstanceResponse createInstance(CreateSurveyInstanceRequest request, Long adminId) {
        SurveyTemplate template = findTemplateOrFail(request.getTemplateId());
        if (!BusinessConstants.SURVEY_STATUS_PUBLISHED.equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "only published templates can be distributed");
        }

        SurveyInstance instance = new SurveyInstance();
        instance.setTenantId(SecurityUtils.getCurrentTenantId());
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
        for (SurveyPage page : pages) {
            SurveyInstancePage ip = new SurveyInstancePage();
            ip.setInstanceId(instance.getId());
            ip.setPageId(page.getId());
            ip.setStatus(BusinessConstants.INSTANCE_STATUS_READY);
            instancePageMapper.insert(ip);
        }

        log.info("Survey instance created: id={} templateId={} assignedTo={}", instance.getId(), template.getId(), request.getAssignedTo());
        return toInstanceResponse(instance);
    }

    @Override
    public List<SurveyInstanceResponse> listUserInstances(Long userId) {
        return instanceMapper.selectList(new LambdaQueryWrapper<SurveyInstance>()
                .eq(SurveyInstance::getAssignedTo, userId)
                .orderByDesc(SurveyInstance::getCreatedDate))
                .stream().map(this::toInstanceResponse).collect(Collectors.toList());
    }

    @Override
    public List<SurveyInstanceResponse> listTemplateInstances(Long templateId) {
        return instanceMapper.selectList(new LambdaQueryWrapper<SurveyInstance>()
                .eq(SurveyInstance::getTemplateId, templateId)
                .orderByDesc(SurveyInstance::getCreatedDate))
                .stream().map(this::toInstanceResponse).collect(Collectors.toList());
    }

    // ── Fill Flow ──

    @Override
    public SurveyFillResponse getFillData(Long instanceId, Long userId) {
        SurveyInstance instance = findInstanceOrFail(instanceId);
        // Verify ownership
        if (!instance.getAssignedTo().equals(userId)) {
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
        response.setPages(templateResponse.getPages());
        response.setHiddenTargets(hidden);
        response.setExistingAnswers(existingAnswers);
        return response;
    }

    @Override
    @Transactional
    public void saveAnswer(Long instanceId, SaveAnswerRequest request, Long userId) {
        SurveyInstance instance = findInstanceOrFail(instanceId);
        if (!instance.getAssignedTo().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (BusinessConstants.INSTANCE_STATUS_SUBMITTED.equals(instance.getStatus())
                || BusinessConstants.INSTANCE_STATUS_COMPLETED.equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.INSTANCE_ALREADY_SUBMITTED);
        }

        // Upsert answer
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

        // Update instance page status: find the page containing this question
        SurveyQuestion question = questionMapper.selectById(request.getQuestionId());
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
    }

    @Override
    @Transactional
    public SurveyInstanceResponse submitSurvey(Long instanceId, SubmitSurveyRequest request, Long userId) {
        SurveyInstance instance = findInstanceOrFail(instanceId);
        if (!instance.getAssignedTo().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (BusinessConstants.INSTANCE_STATUS_SUBMITTED.equals(instance.getStatus())
                || BusinessConstants.INSTANCE_STATUS_COMPLETED.equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.INSTANCE_ALREADY_SUBMITTED);
        }

        // Save any final answers from the submit request
        if (request.getAnswers() != null) {
            for (SaveAnswerRequest ans : request.getAnswers()) {
                saveAnswer(instanceId, ans, userId);
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

    private SurveyInstanceResponse toInstanceResponse(SurveyInstance instance) {
        SurveyInstanceResponse r = new SurveyInstanceResponse();
        r.setId(instance.getId());
        r.setTenantId(instance.getTenantId());
        r.setTitle(instance.getTitle());
        r.setStatus(instance.getStatus());
        r.setAssignedTo(instance.getAssignedTo());
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
