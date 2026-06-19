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
        LambdaQueryWrapper<SlaConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.last("ORDER BY FIELD(priority, 'URGENT', 'HIGH', 'MEDIUM', 'LOW')");
        return slaMapper.selectList(wrapper).stream()
                .map(this::toSlaResponse)
                .collect(Collectors.toList());
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

        // Cross-priority validation: load all SLAs and verify order integrity
        List<SlaConfig> allSlas = slaMapper.selectList(new LambdaQueryWrapper<>());

        // Build effective response/resolution maps, applying the pending change
        Map<String, Integer> responseByPriority = new HashMap<>();
        Map<String, Integer> resolutionByPriority = new HashMap<>();
        for (SlaConfig sla : allSlas) {
            if (sla.getId().equals(id)) {
                responseByPriority.put(sla.getPriority(), request.getResponseMinutes());
                resolutionByPriority.put(sla.getPriority(), request.getResolutionMinutes());
            } else {
                responseByPriority.put(sla.getPriority(), sla.getResponseMinutes());
                resolutionByPriority.put(sla.getPriority(), sla.getResolutionMinutes());
            }
        }

        // Verify ascending chain: stricter priority must have lower minute values
        List<String> ordered = BusinessConstants.SLA_PRIORITY_ORDER;
        for (int i = 0; i < ordered.size() - 1; i++) {
            String curPri = ordered.get(i);
            String nextPri = ordered.get(i + 1);
            Integer curResp = responseByPriority.get(curPri);
            Integer nextResp = responseByPriority.get(nextPri);
            Integer curRes = resolutionByPriority.get(curPri);
            Integer nextRes = resolutionByPriority.get(nextPri);

            if (curResp != null && nextResp != null && curResp >= nextResp) {
                throw new BusinessException(ErrorCode.SLA_PRIORITY_ORDER_VIOLATED,
                        nextPri + " response time (" + nextResp + "min) must be greater than "
                                + curPri + " response time (" + curResp + "min)");
            }
            if (curRes != null && nextRes != null && curRes >= nextRes) {
                throw new BusinessException(ErrorCode.SLA_PRIORITY_ORDER_VIOLATED,
                        nextPri + " resolution time (" + nextRes + "min) must be greater than "
                                + curPri + " resolution time (" + curRes + "min)");
            }
        }

        entity.setResponseMinutes(request.getResponseMinutes());
        entity.setResolutionMinutes(request.getResolutionMinutes());
        entity.setLastModifiedBy(adminId);
        entity.setLastModifiedDate(System.currentTimeMillis());
        slaMapper.updateById(entity);
        return toSlaResponse(entity);
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
