package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.common.constant.BusinessConstants;
import com.ticket.common.constant.ErrorCode;
import com.ticket.common.exception.BusinessException;
import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import com.ticket.entity.*;
import com.ticket.mapper.*;
import com.ticket.service.ConfigService;
import com.ticket.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConfigServiceImpl implements ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigServiceImpl.class);

    private final TicketFieldConfigMapper fieldMapper;
    private final SlaConfigMapper slaMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConfigServiceImpl(TicketFieldConfigMapper fieldMapper, SlaConfigMapper slaMapper) {
        this.fieldMapper = fieldMapper;
        this.slaMapper = slaMapper;
    }

    @Override
    public List<FieldConfigResponse> listFields() {
        LambdaQueryWrapper<TicketFieldConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(TicketFieldConfig::getDisplayOrder);
        return fieldMapper.selectList(wrapper).stream()
                .map(this::toFieldResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FieldConfigResponse createField(CreateFieldRequest request, Long adminId) {
        // Check unique field_key
        LambdaQueryWrapper<TicketFieldConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketFieldConfig::getFieldKey, request.getFieldKey());
        if (fieldMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.FIELD_KEY_DUPLICATE);
        }

        // SINGLE_SELECT must have valid options
        if (BusinessConstants.FIELD_TYPE_SINGLE_SELECT.equals(request.getFieldType())) {
            if (request.getOptions() == null || request.getOptions().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "options are required for SINGLE_SELECT field type");
            }
            validateSelectOptions(request.getOptions());
        }

        TicketFieldConfig entity = new TicketFieldConfig();
        entity.setName(request.getName());
        entity.setFieldKey(request.getFieldKey());
        entity.setFieldType(request.getFieldType());
        entity.setOptions(request.getOptions());
        entity.setRequired(request.getRequired() != null && request.getRequired() ? 1 : 0);
        entity.setActive(request.getActive() != null && request.getActive() ? 1 : 0);
        entity.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : BusinessConstants.DEFAULT_DISPLAY_ORDER);
        entity.setCreatedBy(adminId);
        entity.setCreatedDate(System.currentTimeMillis());
        fieldMapper.insert(entity);
        return toFieldResponse(entity);
    }

    @Override
    @Transactional
    public FieldConfigResponse updateField(Long id, UpdateFieldRequest request, Long adminId) {
        TicketFieldConfig entity = fieldMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.FIELD_NOT_FOUND);
        }
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getFieldType() != null) entity.setFieldType(request.getFieldType());
        if (request.getOptions() != null) {
            if (BusinessConstants.FIELD_TYPE_SINGLE_SELECT.equals(entity.getFieldType()) || BusinessConstants.FIELD_TYPE_SINGLE_SELECT.equals(request.getFieldType())) {
                validateSelectOptions(request.getOptions());
            }
            entity.setOptions(request.getOptions());
        }
        if (request.getRequired() != null) entity.setRequired(request.getRequired() ? 1 : 0);
        if (request.getActive() != null) entity.setActive(request.getActive() ? 1 : 0);
        if (request.getDisplayOrder() != null) entity.setDisplayOrder(request.getDisplayOrder());
        entity.setLastModifiedBy(adminId);
        entity.setLastModifiedDate(System.currentTimeMillis());
        fieldMapper.updateById(entity);
        return toFieldResponse(entity);
    }

    @Override
    @Transactional
    public void deleteField(Long id) {
        if (fieldMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.FIELD_NOT_FOUND);
        }
        fieldMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void reorderFields(ReorderFieldsRequest request) {
        for (ReorderFieldsRequest.ReorderItem item : request.getItems()) {
            TicketFieldConfig entity = fieldMapper.selectById(item.getId());
            if (entity == null) continue;
            entity.setDisplayOrder(item.getDisplayOrder());
            fieldMapper.updateById(entity);
        }
    }

    @Override
    public List<SlaConfigResponse> listSla() {
        Long currentTid = SecurityUtils.getCurrentTenantIdOrNull();
        if (currentTid == null) {
            // Superadmin — sees all raw SLAs
            LambdaQueryWrapper<SlaConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.last("ORDER BY tenant_id, CASE priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 WHEN 'LOW' THEN 4 ELSE 5 END");
            return slaMapper.selectList(wrapper).stream()
                    .map(this::toSlaResponse)
                    .collect(Collectors.toList());
        }
        // Tenant-scoped: effective set (custom overrides system default)
        List<SlaConfig> effective = loadEffectiveSlaSet(currentTid);
        return effective.stream().map(this::toSlaResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SlaConfigResponse updateSla(Long id, UpdateSlaRequest request, Long adminId) {
        // Validate: response minutes must be less than resolution minutes
        if (request.getResponseMinutes() >= request.getResolutionMinutes()) {
            throw new BusinessException(ErrorCode.SLA_RESPONSE_MUST_BE_LESS_THAN_RESOLUTION);
        }

        SlaConfig entity = slaMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.SLA_NOT_FOUND);
        }

        Long currentTid = SecurityUtils.getCurrentTenantIdOrNull();
        boolean isSuperadmin = (currentTid == null);

        // ── Copy-on-write: tenant admin editing a system default creates a new custom SLA ──
        if (entity.getTenantId() == null && !isSuperadmin) {
            SlaConfig custom = new SlaConfig();
            custom.setTenantId(currentTid);
            custom.setPriority(entity.getPriority());
            custom.setResponseMinutes(request.getResponseMinutes());
            custom.setResolutionMinutes(request.getResolutionMinutes());
            custom.setActive(1);
            custom.setCreatedBy(adminId);
            custom.setCreatedDate(System.currentTimeMillis());
            validateSlaChain(currentTid, entity.getPriority(), request.getResponseMinutes(), request.getResolutionMinutes());
            try {
                slaMapper.insert(custom);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // Race: another admin already customized this SLA — update theirs instead
                SlaConfig existing = slaMapper.selectOne(new LambdaQueryWrapper<SlaConfig>()
                        .eq(SlaConfig::getTenantId, currentTid)
                        .eq(SlaConfig::getPriority, entity.getPriority()));
                if (existing != null) {
                    existing.setResponseMinutes(request.getResponseMinutes());
                    existing.setResolutionMinutes(request.getResolutionMinutes());
                    existing.setLastModifiedBy(adminId);
                    existing.setLastModifiedDate(System.currentTimeMillis());
                    slaMapper.updateById(existing);
                    return toSlaResponse(existing);
                }
                throw e; // re-throw if we can't resolve
            }
            return toSlaResponse(custom);
        }

        // ── Access check ──
        if (entity.getTenantId() != null) {
            if (isSuperadmin) {
                // Superadmin can edit any tenant's custom SLA
            } else if (!entity.getTenantId().equals(currentTid)) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
        } else if (!isSuperadmin) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // ── Direct update (superadmin editing system default, or tenant editing own custom) ──
        validateSlaChain(currentTid, entity.getPriority(), request.getResponseMinutes(), request.getResolutionMinutes());
        entity.setResponseMinutes(request.getResponseMinutes());
        entity.setResolutionMinutes(request.getResolutionMinutes());
        entity.setLastModifiedBy(adminId);
        entity.setLastModifiedDate(System.currentTimeMillis());
        slaMapper.updateById(entity);
        return toSlaResponse(entity);
    }

    // ── SLA helpers ──

    /**
     * Load the effective SLA set for a tenant: custom overrides system default per priority.
     */
    private List<SlaConfig> loadEffectiveSlaSet(Long tenantId) {
        LambdaQueryWrapper<SlaConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(SlaConfig::getTenantId)
                .or().eq(SlaConfig::getTenantId, tenantId));
        wrapper.last("ORDER BY CASE priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 WHEN 'LOW' THEN 4 ELSE 5 END");
        List<SlaConfig> all = slaMapper.selectList(wrapper);
        Map<String, SlaConfig> effective = new LinkedHashMap<>();
        // Process system defaults first, then tenant customs (latter overrides)
        for (SlaConfig sla : all) {
            if (sla.getTenantId() == null) {
                effective.putIfAbsent(sla.getPriority(), sla);
            } else {
                effective.put(sla.getPriority(), sla);
            }
        }
        return new ArrayList<>(effective.values());
    }

    /**
     * Validate SLA priority ordering in the effective set for a tenant.
     * Higher priority must have strictly lower response and resolution minutes.
     */
    private void validateSlaChain(Long tenantId, String changedPriority,
                                   int newResponse, int newResolution) {
        List<SlaConfig> effective = loadEffectiveSlaSet(tenantId);
        Map<String, Integer> respMap = new HashMap<>();
        Map<String, Integer> resoMap = new HashMap<>();
        for (SlaConfig sla : effective) {
            if (sla.getPriority().equals(changedPriority)) {
                respMap.put(sla.getPriority(), newResponse);
                resoMap.put(sla.getPriority(), newResolution);
            } else {
                respMap.put(sla.getPriority(), sla.getResponseMinutes());
                resoMap.put(sla.getPriority(), sla.getResolutionMinutes());
            }
        }
        List<String> ordered = BusinessConstants.SLA_PRIORITY_ORDER;
        for (int i = 0; i < ordered.size() - 1; i++) {
            String cur = ordered.get(i);
            String next = ordered.get(i + 1);
            Integer cr = respMap.get(cur);
            Integer nr = respMap.get(next);
            if (cr != null && nr != null && cr >= nr) {
                throw new BusinessException(ErrorCode.SLA_PRIORITY_ORDER_VIOLATED,
                        next + " response (" + nr + "min) must be > " + cur + " response (" + cr + "min)");
            }
            Integer cs = resoMap.get(cur);
            Integer ns = resoMap.get(next);
            if (cs != null && ns != null && cs >= ns) {
                throw new BusinessException(ErrorCode.SLA_PRIORITY_ORDER_VIOLATED,
                        next + " resolution (" + ns + "min) must be > " + cur + " resolution (" + cs + "min)");
            }
        }
    }

    private void validateSelectOptions(String optionsJson) {
        try {
            var node = objectMapper.readTree(optionsJson);
            var items = node.get("items");
            if (items == null || !items.isArray() || items.size() == 0 || items.size() > BusinessConstants.MAX_SELECT_OPTIONS) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "options must contain 'items' array with 1-20 entries");
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "options must be valid JSON");
        }
    }

    private FieldConfigResponse toFieldResponse(TicketFieldConfig entity) {
        FieldConfigResponse resp = new FieldConfigResponse();
        resp.setId(entity.getId());
        resp.setName(entity.getName());
        resp.setFieldKey(entity.getFieldKey());
        resp.setFieldType(entity.getFieldType());
        resp.setOptions(entity.getOptions());
        resp.setRequired(entity.getRequired() == 1);
        resp.setActive(entity.getActive() == 1);
        resp.setDisplayOrder(entity.getDisplayOrder());
        resp.setCreatedDate(entity.getCreatedDate());
        return resp;
    }

    private SlaConfigResponse toSlaResponse(SlaConfig entity) {
        SlaConfigResponse resp = new SlaConfigResponse();
        resp.setId(entity.getId());
        resp.setPriority(entity.getPriority());
        resp.setResponseMinutes(entity.getResponseMinutes());
        resp.setResolutionMinutes(entity.getResolutionMinutes());
        resp.setActive(entity.getActive() == 1);
        return resp;
    }
}
