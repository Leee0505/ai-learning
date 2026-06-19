# Remaining MVP Features — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 4 remaining MVP features: Ticket Configuration, System Monitoring, WebSocket Notifications, Timeout Alerts.

**Architecture:** Each feature follows the same layered pattern: DB migration → Entity → Mapper → Service → Controller → DTOs → Pinia store → Vue view. REST APIs return standard `ApiResult<T>`. Frontend uses Element Plus components with custom CSS variables from the design system.

**Tech Stack:** Spring Boot 3.2 + MyBatis-Plus 3.5 + Vue 3.4 + Element Plus + ECharts + STOMP/WebSocket + Kafka + Redis (Redisson)

---

## File Map

### Feature 1: Ticket Configuration
| Action | File |
|--------|------|
| Create | `backend/src/main/resources/db/migration/V9__create_ticket_config.sql` |
| Create | `backend/src/main/java/com/ticket/entity/TicketFieldConfig.java` |
| Create | `backend/src/main/java/com/ticket/entity/SlaConfig.java` |
| Create | `backend/src/main/java/com/ticket/mapper/TicketFieldConfigMapper.java` |
| Create | `backend/src/main/java/com/ticket/mapper/SlaConfigMapper.java` |
| Create | `backend/src/main/java/com/ticket/dto/request/CreateFieldRequest.java` |
| Create | `backend/src/main/java/com/ticket/dto/request/UpdateFieldRequest.java` |
| Create | `backend/src/main/java/com/ticket/dto/request/ReorderFieldsRequest.java` |
| Create | `backend/src/main/java/com/ticket/dto/request/UpdateSlaRequest.java` |
| Create | `backend/src/main/java/com/ticket/dto/response/FieldConfigResponse.java` |
| Create | `backend/src/main/java/com/ticket/dto/response/SlaConfigResponse.java` |
| Create | `backend/src/main/java/com/ticket/service/ConfigService.java` |
| Create | `backend/src/main/java/com/ticket/service/impl/ConfigServiceImpl.java` |
| Create | `backend/src/main/java/com/ticket/controller/ConfigController.java` |
| Create | `backend/src/test/java/com/ticket/controller/ConfigControllerTest.java` |
| Modify | `backend/src/main/java/com/ticket/common/constant/ErrorCode.java` |
| Modify | `backend/src/main/java/com/ticket/security/SecurityConfig.java` |
| Create | `frontend/src/api/config.js` |
| Create | `frontend/src/stores/config.js` |
| Create | `frontend/src/views/admin/ConfigView.vue` |
| Modify | `frontend/src/router/index.js` |
| Modify | `frontend/src/layouts/AppLayout.vue` |

### Feature 2: System Monitoring
| Action | File |
|--------|------|
| Create | `backend/src/main/java/com/ticket/config/ApiMetricsInterceptor.java` |
| Create | `backend/src/main/java/com/ticket/dto/response/MonitorOverviewResponse.java` |
| Create | `backend/src/main/java/com/ticket/dto/response/KafkaMetricsResponse.java` |
| Create | `backend/src/main/java/com/ticket/dto/response/RedisMetricsResponse.java` |
| Create | `backend/src/main/java/com/ticket/dto/response/ApiMetricsResponse.java` |
| Create | `backend/src/main/java/com/ticket/service/MonitorService.java` |
| Create | `backend/src/main/java/com/ticket/service/impl/MonitorServiceImpl.java` |
| Create | `backend/src/main/java/com/ticket/controller/MonitorController.java` |
| Create | `backend/src/test/java/com/ticket/controller/MonitorControllerTest.java` |
| Modify | `backend/src/main/java/com/ticket/config/WebMvcConfig.java` |
| Modify | `backend/src/main/java/com/ticket/security/SecurityConfig.java` |
| Create | `frontend/src/api/monitor.js` |
| Create | `frontend/src/views/admin/MonitorView.vue` |
| Modify | `frontend/src/router/index.js` |
| Modify | `frontend/src/layouts/AppLayout.vue` |

### Feature 3: WebSocket Notifications
| Action | File |
|--------|------|
| Create | `backend/src/main/resources/db/migration/V10__create_notifications.sql` |
| Create | `backend/src/main/java/com/ticket/entity/Notification.java` |
| Create | `backend/src/main/java/com/ticket/mapper/NotificationMapper.java` |
| Create | `backend/src/main/java/com/ticket/dto/response/NotificationResponse.java` |
| Create | `backend/src/main/java/com/ticket/config/WebSocketConfig.java` |
| Create | `backend/src/main/java/com/ticket/security/WebSocketAuthInterceptor.java` |
| Create | `backend/src/main/java/com/ticket/event/NotificationService.java` |
| Create | `backend/src/main/java/com/ticket/controller/NotificationController.java` |
| Create | `backend/src/test/java/com/ticket/controller/NotificationControllerTest.java` |
| Modify | `backend/src/main/java/com/ticket/event/KafkaEventConsumer.java` |
| Modify | `backend/src/main/java/com/ticket/event/TicketCreatedEvent.java` |
| Modify | `backend/src/main/java/com/ticket/event/TicketAssignedEvent.java` |
| Modify | `backend/src/main/java/com/ticket/security/SecurityConfig.java` |
| Modify | `backend/pom.xml` (add spring-boot-starter-websocket) |
| Create | `frontend/src/api/notifications.js` |
| Create | `frontend/src/stores/notifications.js` |
| Create | `frontend/src/components/NotificationBell.vue` |
| Modify | `frontend/src/layouts/AppLayout.vue` |

### Feature 4: Timeout Alerts
| Action | File |
|--------|------|
| Create | `backend/src/main/java/com/ticket/event/TicketOverdueEvent.java` |
| Create | `backend/src/main/java/com/ticket/task/SlaCheckScheduler.java` |
| Create | `backend/src/test/java/com/ticket/task/SlaCheckSchedulerTest.java` |
| Modify | `backend/src/main/java/com/ticket/event/EventPublisher.java` |
| Modify | `backend/src/main/java/com/ticket/event/KafkaEventPublisher.java` |
| Modify | `backend/src/main/java/com/ticket/event/KafkaEventConsumer.java` |
| Modify | `backend/src/main/java/com/ticket/event/NotificationService.java` |
| Modify | `frontend/src/views/AgentWorkbench.vue` |

---

## Feature 1: Ticket Configuration

### Task 1.1: V9 DB Migration

**Files:**
- Create: `ticket-system/backend/src/main/resources/db/migration/V9__create_ticket_config.sql`

- [ ] **Step 1: Write migration SQL**

```sql
-- V9: Ticket configuration — custom field definitions + SLA rules
CREATE TABLE IF NOT EXISTS ticket_field_config (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    name              VARCHAR(100) NOT NULL COMMENT 'Display name, e.g. Environment',
    field_key         VARCHAR(50) NOT NULL COMMENT 'Unique machine key, e.g. environment',
    field_type        VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT | SINGLE_SELECT | NUMBER | DATE',
    options           JSON DEFAULT NULL COMMENT 'For SELECT: {"items":["prod","staging","dev"]}',
    required          TINYINT NOT NULL DEFAULT 0,
    active            TINYINT NOT NULL DEFAULT 1,
    display_order     INT NOT NULL DEFAULT 0,
    created_by        BIGINT NOT NULL,
    created_date      BIGINT NOT NULL,
    last_modified_by  BIGINT DEFAULT NULL,
    last_modified_date BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_field_key (field_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sla_config (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    priority          VARCHAR(20) NOT NULL COMMENT 'LOW | MEDIUM | HIGH | URGENT',
    response_hours    INT NOT NULL COMMENT 'First response SLA in hours',
    resolution_hours  INT NOT NULL COMMENT 'Resolution SLA in hours',
    active            TINYINT NOT NULL DEFAULT 1,
    created_by        BIGINT NOT NULL,
    created_date      BIGINT NOT NULL,
    last_modified_by  BIGINT DEFAULT NULL,
    last_modified_date BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE ticket ADD COLUMN custom_fields JSON DEFAULT NULL;

-- Seed default SLA rules
INSERT INTO sla_config (priority, response_hours, resolution_hours, created_by, created_date) VALUES
('URGENT', 1, 4, 0, UNIX_TIMESTAMP() * 1000),
('HIGH', 4, 24, 0, UNIX_TIMESTAMP() * 1000),
('MEDIUM', 8, 48, 0, UNIX_TIMESTAMP() * 1000),
('LOW', 24, 96, 0, UNIX_TIMESTAMP() * 1000);

-- Seed default custom fields
INSERT INTO ticket_field_config (name, field_key, field_type, options, display_order, created_by, created_date) VALUES
('Environment', 'environment', 'SINGLE_SELECT', '{"items":["Production","Staging","Development"]}', 1, 0, UNIX_TIMESTAMP() * 1000),
('Version', 'version', 'TEXT', NULL, 2, 0, UNIX_TIMESTAMP() * 1000);
```

- [ ] **Step 2: Commit**

```bash
git add ticket-system/backend/src/main/resources/db/migration/V9__create_ticket_config.sql
git commit -m "feat: V9 migration — ticket_field_config + sla_config tables"
```

### Task 1.2: Entities + Mappers

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/entity/TicketFieldConfig.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/entity/SlaConfig.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/mapper/TicketFieldConfigMapper.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/mapper/SlaConfigMapper.java`

- [ ] **Step 1: Create TicketFieldConfig entity**

```java
package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("ticket_field_config")
public class TicketFieldConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String fieldKey;
    private String fieldType;
    private String options; // JSON string, parsed in service layer
    private Integer required;
    private Integer active;
    private Integer displayOrder;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private Long createdDate;
    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedBy;
    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedDate;
}
```

- [ ] **Step 2: Create SlaConfig entity**

```java
package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("sla_config")
public class SlaConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String priority;
    private Integer responseHours;
    private Integer resolutionHours;
    private Integer active;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private Long createdDate;
    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedBy;
    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedDate;
}
```

- [ ] **Step 3: Create TicketFieldConfigMapper**

```java
package com.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.entity.TicketFieldConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketFieldConfigMapper extends BaseMapper<TicketFieldConfig> {
}
```

- [ ] **Step 4: Create SlaConfigMapper**

```java
package com.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.entity.SlaConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SlaConfigMapper extends BaseMapper<SlaConfig> {
}
```

- [ ] **Step 5: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/entity/TicketFieldConfig.java \
        ticket-system/backend/src/main/java/com/ticket/entity/SlaConfig.java \
        ticket-system/backend/src/main/java/com/ticket/mapper/TicketFieldConfigMapper.java \
        ticket-system/backend/src/main/java/com/ticket/mapper/SlaConfigMapper.java
git commit -m "feat: entities + mappers for ticket field config and SLA"
```

### Task 1.3: DTOs

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/request/CreateFieldRequest.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/request/UpdateFieldRequest.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/request/ReorderFieldsRequest.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/request/UpdateSlaRequest.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/response/FieldConfigResponse.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/response/SlaConfigResponse.java`

- [ ] **Step 1: Create CreateFieldRequest**

```java
package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFieldRequest {
    @NotBlank(message = "field name is required")
    @Size(max = 100, message = "field name must be at most 100 characters")
    private String name;

    @NotBlank(message = "field key is required")
    @Size(max = 50, message = "field key must be at most 50 characters")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "field key must start with a letter and contain only lowercase letters, numbers, and underscores")
    private String fieldKey;

    @NotBlank(message = "field type is required")
    @Pattern(regexp = "^(TEXT|SINGLE_SELECT|NUMBER|DATE)$", message = "field type must be one of TEXT, SINGLE_SELECT, NUMBER, DATE")
    private String fieldType;

    private String options; // JSON string, nullable
    private Boolean required = false;
    private Boolean active = true;
    private Integer displayOrder;
}
```

- [ ] **Step 2: Create UpdateFieldRequest**

```java
package com.ticket.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateFieldRequest {
    @Size(max = 100, message = "field name must be at most 100 characters")
    private String name;

    @Pattern(regexp = "^(TEXT|SINGLE_SELECT|NUMBER|DATE)$", message = "field type must be one of TEXT, SINGLE_SELECT, NUMBER, DATE")
    private String fieldType;

    private String options;
    private Boolean required;
    private Boolean active;
    private Integer displayOrder;
}
```

- [ ] **Step 3: Create ReorderFieldsRequest**

```java
package com.ticket.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReorderFieldsRequest {
    @NotNull
    @Size(min = 1)
    @Valid
    private List<ReorderItem> items;

    @Data
    public static class ReorderItem {
        @NotNull
        private Long id;
        @NotNull
        private Integer displayOrder;
    }
}
```

- [ ] **Step 4: Create UpdateSlaRequest**

```java
package com.ticket.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateSlaRequest {
    @NotNull(message = "response hours is required")
    @Min(value = 1, message = "response hours must be at least 1")
    @Max(value = 720, message = "response hours must be at most 720 (30 days)")
    private Integer responseHours;

    @NotNull(message = "resolution hours is required")
    @Min(value = 1, message = "resolution hours must be at least 1")
    @Max(value = 1440, message = "resolution hours must be at most 1440 (60 days)")
    private Integer resolutionHours;
}
```

- [ ] **Step 5: Create FieldConfigResponse**

```java
package com.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldConfigResponse {
    private Long id;
    private String name;
    private String fieldKey;
    private String fieldType;
    private String options; // JSON string — frontend parses it
    private Boolean required;
    private Boolean active;
    private Integer displayOrder;
    private Long createdDate;
}
```

- [ ] **Step 6: Create SlaConfigResponse**

```java
package com.ticket.dto.response;

import lombok.Data;

@Data
public class SlaConfigResponse {
    private Long id;
    private String priority;
    private Integer responseHours;
    private Integer resolutionHours;
    private Boolean active;
}
```

- [ ] **Step 7: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/dto/request/CreateFieldRequest.java \
        ticket-system/backend/src/main/java/com/ticket/dto/request/UpdateFieldRequest.java \
        ticket-system/backend/src/main/java/com/ticket/dto/request/ReorderFieldsRequest.java \
        ticket-system/backend/src/main/java/com/ticket/dto/request/UpdateSlaRequest.java \
        ticket-system/backend/src/main/java/com/ticket/dto/response/FieldConfigResponse.java \
        ticket-system/backend/src/main/java/com/ticket/dto/response/SlaConfigResponse.java
git commit -m "feat: DTOs for ticket config — field and SLA CRUD"
```

### Task 1.4: Add Error Codes

**Files:**
- Modify: `ticket-system/backend/src/main/java/com/ticket/common/constant/ErrorCode.java`

- [ ] **Step 1: Add config-specific error codes**

In `ErrorCode.java`, add after `KNOWLEDGE_NOT_FOUND`:

```java
    FIELD_KEY_DUPLICATE(40024, "a field with this key already exists"),
    FIELD_NOT_FOUND(40025, "field config not found"),
    SLA_NOT_FOUND(40026, "SLA config not found"),
    SLA_RESPONSE_MUST_BE_LESS_THAN_RESOLUTION(40027, "response hours must be less than resolution hours"),
```

- [ ] **Step 2: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/common/constant/ErrorCode.java
git commit -m "feat: add config-specific error codes"
```

### Task 1.5: ConfigService + Impl

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/service/ConfigService.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/service/impl/ConfigServiceImpl.java`

- [ ] **Step 1: Create ConfigService interface**

```java
package com.ticket.service;

import com.ticket.dto.request.*;
import com.ticket.dto.response.*;

import java.util.List;

public interface ConfigService {
    // Field config
    List<FieldConfigResponse> listFields();
    FieldConfigResponse createField(CreateFieldRequest request, Long adminId);
    FieldConfigResponse updateField(Long id, UpdateFieldRequest request, Long adminId);
    void deleteField(Long id);
    void reorderFields(ReorderFieldsRequest request);

    // SLA config
    List<SlaConfigResponse> listSla();
    SlaConfigResponse updateSla(Long id, UpdateSlaRequest request, Long adminId);
}
```

- [ ] **Step 2: Create ConfigServiceImpl**

```java
package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
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
    public FieldConfigResponse createField(CreateFieldRequest request, Long adminId) {
        // Check unique field_key
        LambdaQueryWrapper<TicketFieldConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketFieldConfig::getFieldKey, request.getFieldKey());
        if (fieldMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.FIELD_KEY_DUPLICATE);
        }

        // Validate options JSON for SINGLE_SELECT
        if ("SINGLE_SELECT".equals(request.getFieldType()) && request.getOptions() != null) {
            validateSelectOptions(request.getOptions());
        }

        TicketFieldConfig entity = new TicketFieldConfig();
        entity.setName(request.getName());
        entity.setFieldKey(request.getFieldKey());
        entity.setFieldType(request.getFieldType());
        entity.setOptions(request.getOptions());
        entity.setRequired(request.getRequired() != null && request.getRequired() ? 1 : 0);
        entity.setActive(request.getActive() != null && request.getActive() ? 1 : 0);
        entity.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 99);
        entity.setCreatedBy(adminId);
        entity.setCreatedDate(System.currentTimeMillis());
        fieldMapper.insert(entity);
        return toFieldResponse(entity);
    }

    @Override
    public FieldConfigResponse updateField(Long id, UpdateFieldRequest request, Long adminId) {
        TicketFieldConfig entity = fieldMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.FIELD_NOT_FOUND);
        }
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getFieldType() != null) entity.setFieldType(request.getFieldType());
        if (request.getOptions() != null) {
            if ("SINGLE_SELECT".equals(entity.getFieldType()) || "SINGLE_SELECT".equals(request.getFieldType())) {
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
        wrapper.apply("1=1 ORDER BY FIELD(priority, 'URGENT', 'HIGH', 'MEDIUM', 'LOW')");
        return slaMapper.selectList(wrapper).stream()
                .map(this::toSlaResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SlaConfigResponse updateSla(Long id, UpdateSlaRequest request, Long adminId) {
        // Validate: response hours must be less than resolution hours
        if (request.getResponseHours() >= request.getResolutionHours()) {
            throw new BusinessException(ErrorCode.SLA_RESPONSE_MUST_BE_LESS_THAN_RESOLUTION);
        }

        SlaConfig entity = slaMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.SLA_NOT_FOUND);
        }
        entity.setResponseHours(request.getResponseHours());
        entity.setResolutionHours(request.getResolutionHours());
        entity.setLastModifiedBy(adminId);
        entity.setLastModifiedDate(System.currentTimeMillis());
        slaMapper.updateById(entity);
        return toSlaResponse(entity);
    }

    private void validateSelectOptions(String optionsJson) {
        try {
            var node = objectMapper.readTree(optionsJson);
            var items = node.get("items");
            if (items == null || !items.isArray() || items.size() == 0 || items.size() > 20) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR.getCode(),
                        "options must contain 'items' array with 1-20 entries");
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR.getCode(), "options must be valid JSON");
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
        resp.setResponseHours(entity.getResponseHours());
        resp.setResolutionHours(entity.getResolutionHours());
        resp.setActive(entity.getActive() == 1);
        return resp;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/service/ConfigService.java \
        ticket-system/backend/src/main/java/com/ticket/service/impl/ConfigServiceImpl.java
git commit -m "feat: ConfigService — field and SLA CRUD with validation"
```

### Task 1.6: ConfigController

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/controller/ConfigController.java`

- [ ] **Step 1: Create ConfigController**

```java
package com.ticket.controller;

import com.ticket.common.constant.RoleConstants;
import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/config")
@PreAuthorize("hasRole('" + RoleConstants.ADMIN + "')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Config", description = "Admin ticket configuration — custom fields and SLA rules")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    // ── Custom Fields ──

    @GetMapping("/fields")
    @Operation(summary = "List all custom field definitions")
    public ApiResult<List<FieldConfigResponse>> listFields() {
        return ApiResult.success(configService.listFields());
    }

    @PostMapping("/fields")
    @Operation(summary = "Create a custom field definition")
    public ApiResult<FieldConfigResponse> createField(
            @Valid @RequestBody CreateFieldRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        return ApiResult.success(configService.createField(request, admin.getUserId()));
    }

    @PutMapping("/fields/{id}")
    @Operation(summary = "Update a custom field definition")
    public ApiResult<FieldConfigResponse> updateField(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFieldRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        return ApiResult.success(configService.updateField(id, request, admin.getUserId()));
    }

    @DeleteMapping("/fields/{id}")
    @Operation(summary = "Delete a custom field definition")
    public ApiResult<Void> deleteField(@PathVariable Long id) {
        configService.deleteField(id);
        return ApiResult.success();
    }

    @PutMapping("/fields/reorder")
    @Operation(summary = "Batch reorder custom fields")
    public ApiResult<Void> reorderFields(@Valid @RequestBody ReorderFieldsRequest request) {
        configService.reorderFields(request);
        return ApiResult.success();
    }

    // ── SLA Rules ──

    @GetMapping("/sla")
    @Operation(summary = "List all SLA rules")
    public ApiResult<List<SlaConfigResponse>> listSla() {
        return ApiResult.success(configService.listSla());
    }

    @PutMapping("/sla/{id}")
    @Operation(summary = "Update an SLA rule")
    public ApiResult<SlaConfigResponse> updateSla(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSlaRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        return ApiResult.success(configService.updateSla(id, request, admin.getUserId()));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/controller/ConfigController.java
git commit -m "feat: ConfigController — admin endpoints for field & SLA config"
```

### Task 1.7: Backend Unit Tests

**Files:**
- Create: `ticket-system/backend/src/test/java/com/ticket/controller/ConfigControllerTest.java`

- [ ] **Step 1: Write ConfigControllerTest**

```java
package com.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.common.constant.ErrorCode;
import com.ticket.common.exception.BusinessException;
import com.ticket.config.TestConfig;
import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import com.ticket.entity.*;
import com.ticket.mapper.*;
import com.ticket.service.ConfigService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@Sql(scripts = "/sql/init-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class ConfigControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // ── Field Tests ──

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldListFields() throws Exception {
        mockMvc.perform(get("/api/admin/config/fields"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateField() throws Exception {
        CreateFieldRequest req = new CreateFieldRequest();
        req.setName("Test Field");
        req.setFieldKey("test_field");
        req.setFieldType("TEXT");
        req.setDisplayOrder(10);

        mockMvc.perform(post("/api/admin/config/fields")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Test Field"))
                .andExpect(jsonPath("$.data.fieldKey").value("test_field"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectDuplicateFieldKey() throws Exception {
        CreateFieldRequest req = new CreateFieldRequest();
        req.setName("Env");
        req.setFieldKey("environment"); // Already seeded
        req.setFieldType("TEXT");

        mockMvc.perform(post("/api/admin/config/fields")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.FIELD_KEY_DUPLICATE.getCode()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteField() throws Exception {
        // First create one
        CreateFieldRequest req = new CreateFieldRequest();
        req.setName("ToDelete");
        req.setFieldKey("to_delete");
        req.setFieldType("TEXT");

        String response = mockMvc.perform(post("/api/admin/config/fields")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int id = objectMapper.readTree(response).get("data").get("id").asInt();

        mockMvc.perform(delete("/api/admin/config/fields/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ── SLA Tests ──

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldListSla() throws Exception {
        mockMvc.perform(get("/api/admin/config/sla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(4)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateSla() throws Exception {
        UpdateSlaRequest req = new UpdateSlaRequest();
        req.setResponseHours(2);
        req.setResolutionHours(8);

        mockMvc.perform(put("/api/admin/config/sla/1") // URGENT
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.responseHours").value(2))
                .andExpect(jsonPath("$.data.resolutionHours").value(8));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectSlaWhereResponseExceedsResolution() throws Exception {
        UpdateSlaRequest req = new UpdateSlaRequest();
        req.setResponseHours(10);
        req.setResolutionHours(5);

        mockMvc.perform(put("/api/admin/config/sla/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SLA_RESPONSE_MUST_BE_LESS_THAN_RESOLUTION.getCode()));
    }

    // ── Access Control ──

    @Test
    @WithMockUser(roles = "USER")
    void shouldDenyNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/config/fields"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run tests**

```bash
cd ticket-system/backend && mvn test -Dtest=ConfigControllerTest -pl . 2>&1 | tail -30
```

Expected: 7 tests pass

- [ ] **Step 3: Commit**

```bash
git add ticket-system/backend/src/test/java/com/ticket/controller/ConfigControllerTest.java
git commit -m "test: ConfigController — field and SLA CRUD tests"
```

### Task 1.8: Frontend API + Store

**Files:**
- Create: `ticket-system/frontend/src/api/config.js`
- Create: `ticket-system/frontend/src/stores/config.js`

- [ ] **Step 1: Create config API**

```javascript
// ticket-system/frontend/src/api/config.js
import request from './request'

export function getFieldsApi() {
  return request.get('/admin/config/fields')
}

export function createFieldApi(data) {
  return request.post('/admin/config/fields', data)
}

export function updateFieldApi(id, data) {
  return request.put(`/admin/config/fields/${id}`, data)
}

export function deleteFieldApi(id) {
  return request.delete(`/admin/config/fields/${id}`)
}

export function reorderFieldsApi(data) {
  return request.put('/admin/config/fields/reorder', data)
}

export function getSlaApi() {
  return request.get('/admin/config/sla')
}

export function updateSlaApi(id, data) {
  return request.put(`/admin/config/sla/${id}`, data)
}
```

- [ ] **Step 2: Create config store**

```javascript
// ticket-system/frontend/src/stores/config.js
import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getFieldsApi, createFieldApi, updateFieldApi,
  deleteFieldApi, reorderFieldsApi,
  getSlaApi, updateSlaApi
} from '@/api/config'

export const useConfigStore = defineStore('config', () => {
  const fields = ref([])
  const slaRules = ref([])
  const loading = ref(false)

  async function fetchFields() {
    loading.value = true
    try {
      const { data } = await getFieldsApi()
      if (data?.code === 200) {
        fields.value = data.data || []
      }
    } catch (e) {
      console.error('[config store] fetchFields:', e)
    } finally {
      loading.value = false
    }
  }

  async function createField(fieldData) {
    const { data } = await createFieldApi(fieldData)
    if (data.code === 200) {
      await fetchFields()
    }
    return data
  }

  async function updateField(id, fieldData) {
    const { data } = await updateFieldApi(id, fieldData)
    if (data.code === 200) {
      await fetchFields()
    }
    return data
  }

  async function deleteField(id) {
    const { data } = await deleteFieldApi(id)
    if (data.code === 200) {
      await fetchFields()
    }
    return data
  }

  async function reorderFields(items) {
    const { data } = await reorderFieldsApi({ items })
    if (data.code === 200) {
      await fetchFields()
    }
    return data
  }

  async function fetchSla() {
    try {
      const { data } = await getSlaApi()
      if (data?.code === 200) {
        slaRules.value = data.data || []
      }
    } catch (e) {
      console.error('[config store] fetchSla:', e)
    }
  }

  async function updateSla(id, slaData) {
    const { data } = await updateSlaApi(id, slaData)
    if (data.code === 200) {
      await fetchSla()
    }
    return data
  }

  return {
    fields, slaRules, loading,
    fetchFields, createField, updateField, deleteField, reorderFields,
    fetchSla, updateSla
  }
})
```

- [ ] **Step 3: Commit**

```bash
git add ticket-system/frontend/src/api/config.js \
        ticket-system/frontend/src/stores/config.js
git commit -m "feat: frontend API + store for ticket config"
```

### Task 1.9: ConfigView.vue — Template + Tabs

**Files:**
- Create: `ticket-system/frontend/src/views/admin/ConfigView.vue`

- [ ] **Step 1: Create ConfigView with Custom Fields tab**

```vue
<template>
  <div class="config-page">
    <header class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">Configuration</h1>
        <p class="page-subtitle">Manage custom ticket fields and SLA rules</p>
      </div>
    </header>

    <div class="config-tabs" role="tablist">
      <button
        role="tab"
        :aria-selected="activeTab === 'fields'"
        class="tab-btn"
        :class="{ 'tab-btn--active': activeTab === 'fields' }"
        @click="activeTab = 'fields'"
      >Custom Fields</button>
      <button
        role="tab"
        :aria-selected="activeTab === 'sla'"
        class="tab-btn"
        :class="{ 'tab-btn--active': activeTab === 'sla' }"
        @click="activeTab = 'sla'"
      >SLA Rules</button>
    </div>

    <!-- ── Custom Fields Tab ── -->
    <div v-if="activeTab === 'fields'" role="tabpanel" class="tab-panel">
      <div v-if="store.loading && store.fields.length === 0" class="skeleton-list">
        <div v-for="i in 4" :key="i" class="skeleton-row">
          <div class="skeleton" style="width:24px;height:24px"></div>
          <div class="skeleton" style="flex:1;height:20px"></div>
          <div class="skeleton" style="width:100px;height:20px"></div>
          <div class="skeleton" style="width:48px;height:32px"></div>
        </div>
      </div>

      <div v-else-if="!store.loading && store.fields.length === 0" class="empty-state">
        <p class="empty-title">No custom fields configured</p>
        <p class="empty-desc">Add your first field to enhance ticket forms</p>
        <button class="btn-primary" @click="openAddDialog">Add Field</button>
      </div>

      <div v-else class="card">
        <div class="card-header">
          <span class="card-count">{{ store.fields.length }} field{{ store.fields.length !== 1 ? 's' : '' }}</span>
          <button class="btn-primary" @click="openAddDialog">+ Add Field</button>
        </div>
        <div class="field-list">
          <div
            v-for="(field, i) in store.fields"
            :key="field.id"
            class="field-row"
            :class="{ 'field-row--selected': selectedField?.id === field.id }"
            @click="selectedField = field"
          >
            <span class="drag-handle" title="Drag to reorder" aria-label="Drag to reorder">⠿</span>
            <div class="field-info">
              <span class="field-name">{{ field.name }}</span>
              <span class="field-key">{{ field.fieldKey }}</span>
            </div>
            <span class="badge badge--type">{{ field.fieldType }}</span>
            <el-switch
              :model-value="field.active"
              @change="(val) => handleToggleActive(field, val)"
              @click.stop
              :aria-label="`${field.active ? 'Disable' : 'Enable'} ${field.name}`"
            />
            <button
              class="act-btn act-btn--edit"
              title="Edit"
              @click.stop="openEditDialog(field)"
              aria-label="Edit field"
            >✎</button>
            <button
              class="act-btn act-btn--delete"
              title="Delete"
              @click.stop="handleDelete(field)"
              aria-label="Delete field"
            >✕</button>
          </div>
        </div>

        <!-- Mini preview for selected field -->
        <div v-if="selectedField" class="field-preview">
          <p class="preview-label">Preview — how this field appears in the ticket form</p>
          <div class="preview-box">
            <label class="preview-field-label">{{ selectedField.name }}
              <span v-if="selectedField.required" class="required">*</span>
            </label>
            <input v-if="selectedField.fieldType === 'TEXT'" type="text" class="input" disabled :placeholder="`Enter ${selectedField.name.toLowerCase()}`" />
            <select v-else-if="selectedField.fieldType === 'SINGLE_SELECT'" class="input" disabled>
              <option v-for="item in parseSelectOptions(selectedField.options)" :key="item">{{ item }}</option>
            </select>
            <input v-else-if="selectedField.fieldType === 'NUMBER'" type="number" class="input" disabled :placeholder="`Enter ${selectedField.name.toLowerCase()}`" />
            <input v-else-if="selectedField.fieldType === 'DATE'" type="date" class="input" disabled />
          </div>
        </div>
      </div>
    </div>

    <!-- ── SLA Rules Tab ── -->
    <div v-if="activeTab === 'sla'" role="tabpanel" class="tab-panel">
      <div class="card">
        <table class="sla-table">
          <thead>
            <tr>
              <th>Priority</th><th>Response Time</th><th>Resolution Time</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rule in store.slaRules" :key="rule.id">
              <td><span class="badge" :class="'badge--' + rule.priority.toLowerCase()">{{ rule.priority }}</span></td>
              <td>{{ rule.responseHours }}h</td>
              <td>{{ rule.resolutionHours }}h</td>
              <td>
                <button class="act-btn act-btn--edit" title="Edit SLA" @click="openSlaEdit(rule)" aria-label="Edit SLA rule">✎</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ── Add/Edit Field Dialog ── -->
    <transition name="modal-fade">
      <div v-if="dialogVisible" class="modal-overlay" @click.self="dialogVisible = false">
        <div class="modal" role="dialog" aria-modal="true">
          <div class="modal-header">
            <h2 class="modal-title">{{ editingField ? 'Edit Field' : 'Add Field' }}</h2>
            <button class="modal-close" @click="dialogVisible = false" aria-label="Close">✕</button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label class="form-label" for="field-name">Display Name <span class="required">*</span></label>
              <input id="field-name" v-model="form.name" type="text" class="input" placeholder="e.g. Environment" maxlength="100" />
            </div>
            <div class="form-group">
              <label class="form-label" for="field-key">Field Key <span class="required">*</span></label>
              <input id="field-key" v-model="form.fieldKey" type="text" class="input" placeholder="e.g. environment" maxlength="50" :disabled="!!editingField" />
              <p class="form-hint">Lowercase letters, numbers, and underscores only. Cannot change after creation.</p>
            </div>
            <div class="form-group">
              <label class="form-label" for="field-type">Type</label>
              <select id="field-type" v-model="form.fieldType" class="input">
                <option value="TEXT">Text</option>
                <option value="SINGLE_SELECT">Single Select</option>
                <option value="NUMBER">Number</option>
                <option value="DATE">Date</option>
              </select>
            </div>
            <div v-if="form.fieldType === 'SINGLE_SELECT'" class="form-group">
              <label class="form-label">Options (comma-separated)</label>
              <input v-model="optionsText" type="text" class="input" placeholder="Production, Staging, Development" />
            </div>
            <div class="form-group">
              <label class="form-checkbox">
                <input v-model="form.required" type="checkbox" />
                Required field
              </label>
            </div>
            <p v-if="dialogError" class="form-error">{{ dialogError }}</p>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="dialogVisible = false">Cancel</button>
            <button class="btn-primary" :disabled="!formValid" @click="handleSaveField">
              {{ editingField ? 'Save Changes' : 'Create Field' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ── Edit SLA Dialog ── -->
    <transition name="modal-fade">
      <div v-if="slaDialogVisible" class="modal-overlay" @click.self="slaDialogVisible = false">
        <div class="modal" role="dialog" aria-modal="true">
          <div class="modal-header">
            <h2 class="modal-title">Edit SLA — {{ slaForm.priority }}</h2>
            <button class="modal-close" @click="slaDialogVisible = false" aria-label="Close">✕</button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label class="form-label" for="sla-response">Response Time (hours)</label>
              <input id="sla-response" v-model.number="slaForm.responseHours" type="number" class="input" min="1" max="720" />
            </div>
            <div class="form-group">
              <label class="form-label" for="sla-resolution">Resolution Time (hours)</label>
              <input id="sla-resolution" v-model.number="slaForm.resolutionHours" type="number" class="input" min="1" max="1440" />
            </div>
            <p v-if="slaDialogError" class="form-error">{{ slaDialogError }}</p>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="slaDialogVisible = false">Cancel</button>
            <button class="btn-primary" :disabled="!slaFormValid" @click="handleSaveSla">Save Changes</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useConfigStore } from '@/stores/config'

const store = useConfigStore()

const activeTab = ref('fields')
const selectedField = ref(null)

// Field dialog
const dialogVisible = ref(false)
const editingField = ref(null)
const form = ref({ name: '', fieldKey: '', fieldType: 'TEXT', required: false })
const optionsText = ref('')
const dialogError = ref('')

const formValid = computed(() => {
  if (!form.value.name.trim()) return false
  if (!editingField.value && !form.value.fieldKey.trim()) return false
  if (form.value.fieldType === 'SINGLE_SELECT' && !optionsText.value.trim()) return false
  return true
})

// SLA dialog
const slaDialogVisible = ref(false)
const slaForm = ref({ id: null, priority: '', responseHours: 0, resolutionHours: 0 })
const slaDialogError = ref('')
const slaFormValid = computed(() =>
  slaForm.value.responseHours > 0 && slaForm.value.resolutionHours > 0 &&
  slaForm.value.responseHours < slaForm.value.resolutionHours
)

onMounted(() => {
  store.fetchFields()
  store.fetchSla()
})

function parseSelectOptions(optionsJson) {
  try {
    const obj = JSON.parse(optionsJson || '{}')
    return obj.items || []
  } catch { return [] }
}

// ── Field Actions ──
function openAddDialog() {
  editingField.value = null
  form.value = { name: '', fieldKey: '', fieldType: 'TEXT', required: false }
  optionsText.value = ''
  dialogError.value = ''
  dialogVisible.value = true
}

function openEditDialog(field) {
  editingField.value = field
  form.value = {
    name: field.name,
    fieldKey: field.fieldKey,
    fieldType: field.fieldType,
    required: field.required
  }
  optionsText.value = parseSelectOptions(field.options).join(', ')
  dialogError.value = ''
  dialogVisible.value = true
}

async function handleSaveField() {
  dialogError.value = ''
  const payload = {
    name: form.value.name.trim(),
    fieldType: form.value.fieldType,
    required: form.value.required
  }
  if (form.value.fieldType === 'SINGLE_SELECT') {
    const items = optionsText.value.split(',').map(s => s.trim()).filter(Boolean)
    payload.options = JSON.stringify({ items })
  }
  if (!editingField.value) {
    payload.fieldKey = form.value.fieldKey.trim()
    const result = await store.createField(payload)
    if (result.code === 200) {
      dialogVisible.value = false
      ElMessage.success('Field created')
    } else {
      dialogError.value = result.message || 'Create failed'
    }
  } else {
    const result = await store.updateField(editingField.value.id, payload)
    if (result.code === 200) {
      dialogVisible.value = false
      ElMessage.success('Field updated')
    } else {
      dialogError.value = result.message || 'Update failed'
    }
  }
}

async function handleDelete(field) {
  try {
    await ElMessageBox.confirm(
      `Delete field "${field.name}"? This will remove the field from ticket forms.`,
      'Confirm Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
  } catch { return }
  try {
    const result = await store.deleteField(field.id)
    if (result.code === 200) {
      if (selectedField.value?.id === field.id) selectedField.value = null
      ElMessage.success('Field deleted')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to delete field')
  }
}

async function handleToggleActive(field, active) {
  try {
    const result = await store.updateField(field.id, { active })
    if (result.code !== 200) {
      ElMessage.error(result.message)
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to update field')
  }
}

// ── SLA Actions ──
function openSlaEdit(rule) {
  slaForm.value = {
    id: rule.id,
    priority: rule.priority,
    responseHours: rule.responseHours,
    resolutionHours: rule.resolutionHours
  }
  slaDialogError.value = ''
  slaDialogVisible.value = true
}

async function handleSaveSla() {
  slaDialogError.value = ''
  try {
    await ElMessageBox.confirm(
      `Change SLA for ${slaForm.value.priority}: response ${slaForm.value.responseHours}h / resolution ${slaForm.value.resolutionHours}h?`,
      'Confirm SLA Update',
      { confirmButtonText: 'Update', cancelButtonText: 'Cancel', type: 'warning' }
    )
  } catch { return }
  try {
    const result = await store.updateSla(slaForm.value.id, {
      responseHours: slaForm.value.responseHours,
      resolutionHours: slaForm.value.resolutionHours
    })
    if (result.code === 200) {
      slaDialogVisible.value = false
      ElMessage.success('SLA updated')
    } else {
      slaDialogError.value = result.message || 'Update failed'
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to update SLA')
  }
}
</script>

<style scoped>
/* ── Page Layout ── */
.config-page { max-width: 960px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
.page-header { margin-bottom: var(--space-lg); }
.page-header-left { display: flex; flex-direction: column; gap: var(--space-xs); }
.page-title { font-family: var(--font-heading); font-size: var(--text-2xl); font-weight: 700; color: var(--color-text-primary); margin: 0; }
.page-subtitle { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0; }

/* ── Tabs ── */
.config-tabs { display: flex; gap: 0; border-bottom: 2px solid var(--color-gray-200); margin-bottom: var(--space-lg); }
.tab-btn { padding: 10px 20px; font-size: var(--text-sm); font-weight: 500; font-family: var(--font-body); color: var(--color-text-secondary); background: none; border: none; border-bottom: 2px solid transparent; margin-bottom: -2px; cursor: pointer; transition: color 150ms, border-color 150ms; }
.tab-btn:hover { color: var(--color-primary); }
.tab-btn--active { color: var(--color-primary); border-bottom-color: var(--color-primary); font-weight: 600; }
.tab-panel { animation: fadeIn 200ms ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }

/* ── Card ── */
.card { background: var(--color-white); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); }
.card-header { display: flex; align-items: center; justify-content: space-between; padding: var(--space-md) var(--space-lg); border-bottom: 1px solid var(--color-gray-100); }
.card-count { font-size: var(--text-sm); color: var(--color-text-muted); }

/* ── Field List ── */
.field-list { }
.field-row { display: flex; align-items: center; gap: var(--space-md); padding: 12px var(--space-lg); border-bottom: 1px solid var(--color-gray-100); cursor: pointer; transition: background var(--transition-fast); }
.field-row:hover { background: var(--color-primary-bg); }
.field-row--selected { background: #F5F3FF; }
.drag-handle { display: flex; align-items: center; justify-content: center; width: 44px; height: 44px; font-size: 20px; color: var(--color-gray-400); cursor: grab; user-select: none; }
.drag-handle:active { cursor: grabbing; transform: scale(0.95); }
.field-info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.field-name { font-weight: 500; font-size: var(--text-sm); color: var(--color-text-primary); }
.field-key { font-family: var(--font-mono); font-size: var(--text-xs); color: var(--color-text-muted); }
.badge--type { font-size: var(--text-xs); font-weight: 600; color: var(--color-primary-dark); background: var(--color-primary-bg); padding: 2px 8px; border-radius: var(--radius-full); }

/* ── Field Preview ── */
.field-preview { border-top: 2px solid var(--color-primary-bg); padding: var(--space-md) var(--space-lg); }
.preview-label { font-size: var(--text-xs); color: var(--color-text-muted); margin: 0 0 var(--space-sm); }
.preview-box { max-width: 360px; padding: var(--space-md); background: var(--color-gray-50); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); }
.preview-field-label { display: block; font-size: var(--text-sm); font-weight: 500; margin-bottom: 6px; color: var(--color-text-secondary); }

/* ── SLA Table ── */
.sla-table { width: 100%; border-collapse: collapse; }
.sla-table thead { background: var(--color-gray-50); border-bottom: 1px solid var(--color-gray-200); }
.sla-table th { padding: 12px 20px; font-size: var(--text-xs); font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; color: var(--color-text-secondary); text-align: left; }
.sla-table td { padding: 12px 20px; font-size: var(--text-sm); color: var(--color-text-primary); border-bottom: 1px solid var(--color-gray-100); font-family: var(--font-mono); }

/* Action buttons */
.act-btn { display: inline-flex; align-items: center; justify-content: center; width: 44px; height: 44px; padding: 0; background: none; border: none; border-radius: var(--radius-md); color: var(--color-text-secondary); cursor: pointer; font-size: 16px; transition: color 150ms, background 150ms; }
.act-btn--edit:hover { color: var(--color-primary); background: var(--color-primary-bg); }
.act-btn--delete:hover { color: var(--color-danger); background: #FEE2E2; }

/* ── Skeleton ── */
.skeleton-list { padding: var(--space-md); display: flex; flex-direction: column; gap: 10px; }
.skeleton-row { display: flex; align-items: center; gap: var(--space-md); }
.skeleton { background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-200) 50%, var(--color-gray-100) 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: var(--radius-sm); }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }

/* ── Empty state ── */
.empty-state { display: flex; flex-direction: column; align-items: center; padding: var(--space-3xl); text-align: center; }
.empty-title { font-size: var(--text-lg); font-weight: 600; color: var(--color-text-primary); margin: 0 0 var(--space-xs); }
.empty-desc { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0 0 var(--space-lg); }

/* ── Modal (reuse from UserListView pattern) ── */
.modal-overlay { position: fixed; inset: 0; z-index: var(--z-modal); display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.5); backdrop-filter: blur(4px); }
.modal { background: var(--color-white); border-radius: var(--radius-xl); box-shadow: var(--shadow-xl); width: 90%; max-width: 480px; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: var(--space-lg) var(--space-lg) 0; }
.modal-title { font-family: var(--font-heading); font-size: var(--text-xl); font-weight: 600; margin: 0; }
.modal-close { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; padding: 0; background: none; border: none; border-radius: var(--radius-md); cursor: pointer; }
.modal-close:hover { background: var(--color-gray-100); }
.modal-body { padding: var(--space-lg); display: flex; flex-direction: column; gap: var(--space-md); }
.modal-footer { display: flex; justify-content: flex-end; gap: var(--space-sm); padding: 0 var(--space-lg) var(--space-lg); }

.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: var(--text-sm); font-weight: 500; color: var(--color-text-secondary); }
.form-hint { font-size: var(--text-xs); color: var(--color-text-muted); margin: 0; }
.form-error { font-size: var(--text-xs); color: var(--color-danger); margin: 0; }
.form-checkbox { display: flex; align-items: center; gap: 8px; font-size: var(--text-sm); color: var(--color-text-secondary); cursor: pointer; }
.form-checkbox input[type="checkbox"] { width: 16px; height: 16px; cursor: pointer; }
.required { color: var(--color-danger); }
.input { padding: 10px 14px; font-size: var(--text-sm); font-family: var(--font-body); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); width: 100%; box-sizing: border-box; }
.input:focus { border-color: var(--color-primary); outline: none; box-shadow: 0 0 0 3px #7C3AED20; }

.btn-primary { display: inline-flex; align-items: center; gap: 6px; padding: 10px 20px; font-size: var(--text-sm); font-weight: 600; color: var(--color-white); background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%); border: none; border-radius: var(--radius-md); cursor: pointer; }
.btn-primary:hover:not(:disabled) { opacity: 0.92; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(124,58,237,0.35); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { padding: 10px 20px; font-size: var(--text-sm); font-weight: 600; background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }
.btn-secondary:hover { border-color: var(--color-primary); color: var(--color-primary); }

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 150ms; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }

@media (max-width: 768px) {
  .config-page { padding: var(--space-lg) var(--space-md); }
  .field-row { flex-wrap: wrap; padding: 12px var(--space-md); }
}
</style>
```

- [ ] **Step 2: Add route and navigation**

In `frontend/src/router/index.js`, add route inside children array after admin/users:

```javascript
      {
        path: 'admin/config',
        name: 'AdminConfig',
        component: () => import('@/views/admin/ConfigView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
```

In `frontend/src/layouts/AppLayout.vue`, add nav link after Users link:

```html
          <router-link v-if="authStore.isAdmin" to="/admin/config" class="app-nav-link" :class="{ 'app-nav-link--active': $route.path.startsWith('/admin/config') }">
            Configuration
          </router-link>
```

- [ ] **Step 3: Commit**

```bash
git add ticket-system/frontend/src/views/admin/ConfigView.vue \
        ticket-system/frontend/src/router/index.js \
        ticket-system/frontend/src/layouts/AppLayout.vue
git commit -m "feat: ConfigView — admin configuration page with fields + SLA tabs"
```

---

## Feature 2: System Monitoring

### Task 2.1: ApiMetricsInterceptor

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/config/ApiMetricsInterceptor.java`
- Modify: `ticket-system/backend/src/main/java/com/ticket/config/WebMvcConfig.java`

- [ ] **Step 1: Create ApiMetricsInterceptor**

```java
package com.ticket.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.LongStream;

@Component
public class ApiMetricsInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiMetricsInterceptor.class);
    private static final int MAX_SAMPLES = 10_000;

    private final ConcurrentLinkedDeque<Long> latencies = new ConcurrentLinkedDeque<>();
    private long requestCount = 0;
    private long errorCount = 0;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("metricsStartTime", System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startNanos = (Long) request.getAttribute("metricsStartTime");
        if (startNanos == null) return;

        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
        synchronized (this) { requestCount++; }
        if (response.getStatus() >= 400) { synchronized (this) { errorCount++; } }

        latencies.addLast(latencyMs);
        // Trim to MAX_SAMPLES
        while (latencies.size() > MAX_SAMPLES) {
            latencies.pollFirst();
        }
    }

    public long getRequestCount() { return requestCount; }
    public long getErrorCount() { return errorCount; }

    public long[] getPercentiles() {
        long[] sorted = latencies.stream().mapToLong(Long::longValue).sorted().toArray();
        if (sorted.length == 0) return new long[]{0, 0, 0};
        return new long[]{
            sorted[(int) (sorted.length * 0.50)],
            sorted[(int) (sorted.length * 0.95)],
            sorted[(int) (sorted.length * 0.99)]
        };
    }
}
```

- [ ] **Step 2: Register interceptor in WebMvcConfig**

```java
// In WebMvcConfig.java, add field and override addInterceptors:

    private final ApiMetricsInterceptor apiMetricsInterceptor;

    public WebMvcConfig(ApiMetricsInterceptor apiMetricsInterceptor) {
        this.apiMetricsInterceptor = apiMetricsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiMetricsInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/admin/monitor/**");
    }
```

- [ ] **Step 3: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/config/ApiMetricsInterceptor.java \
        ticket-system/backend/src/main/java/com/ticket/config/WebMvcConfig.java
git commit -m "feat: ApiMetricsInterceptor — track API latency P50/P95/P99"
```

### Task 2.2: Monitor DTOs + Service + Controller

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/response/MonitorOverviewResponse.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/response/KafkaMetricsResponse.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/response/RedisMetricsResponse.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/response/ApiMetricsResponse.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/service/MonitorService.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/service/impl/MonitorServiceImpl.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/controller/MonitorController.java`

- [ ] **Step 1: Create MonitorOverviewResponse**

```java
package com.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonitorOverviewResponse {
    private String kafkaStatus;  // HEALTHY, WARNING, CRITICAL
    private String redisStatus;
    private String apiStatus;
}
```

- [ ] **Step 2: Create KafkaMetricsResponse**

```java
package com.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KafkaMetricsResponse {
    private boolean connected;
    private int queueDepth;
    private long consumerLag;
    private int activeConsumers;
    private int totalPartitions;
}
```

- [ ] **Step 3: Create RedisMetricsResponse**

```java
package com.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RedisMetricsResponse {
    private boolean connected;
    private double hitRate;       // 0.0–1.0
    private long totalKeys;
    private long evictions;
    private long usedMemoryBytes;
}
```

- [ ] **Step 4: Create ApiMetricsResponse**

```java
package com.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiMetricsResponse {
    private long requestCount;
    private long errorCount;
    private long p50Ms;
    private long p95Ms;
    private long p99Ms;
}
```

- [ ] **Step 5: Create MonitorService interface**

```java
package com.ticket.service;

import com.ticket.dto.response.*;

public interface MonitorService {
    MonitorOverviewResponse getOverview();
    KafkaMetricsResponse getKafkaMetrics();
    RedisMetricsResponse getRedisMetrics();
    ApiMetricsResponse getApiMetrics();
}
```

- [ ] **Step 6: Create MonitorServiceImpl**

```java
package com.ticket.service.impl;

import com.ticket.config.ApiMetricsInterceptor;
import com.ticket.dto.response.*;
import com.ticket.service.MonitorService;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Service
public class MonitorServiceImpl implements MonitorService {

    private static final Logger log = LoggerFactory.getLogger(MonitorServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ApiMetricsInterceptor apiMetrics;
    private final String kafkaBootstrapServers;

    public MonitorServiceImpl(RedisTemplate<String, Object> redisTemplate,
                              ApiMetricsInterceptor apiMetrics,
                              @org.springframework.beans.factory.annotation.Value("${spring.kafka.bootstrap-servers}") String kafkaBootstrapServers) {
        this.redisTemplate = redisTemplate;
        this.apiMetrics = apiMetrics;
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    @Override
    public MonitorOverviewResponse getOverview() {
        KafkaMetricsResponse kafka = getKafkaMetrics();
        RedisMetricsResponse redis = getRedisMetrics();
        ApiMetricsResponse api = getApiMetrics();

        return MonitorOverviewResponse.builder()
                .kafkaStatus(kafka.isConnected() ? (kafka.getConsumerLag() < 10 ? "HEALTHY" : kafka.getConsumerLag() < 100 ? "WARNING" : "CRITICAL") : "CRITICAL")
                .redisStatus(redis.isConnected() ? (redis.getHitRate() > 0.7 ? "HEALTHY" : redis.getHitRate() > 0.5 ? "WARNING" : "CRITICAL") : "CRITICAL")
                .apiStatus(api.getP95Ms() < 200 ? "HEALTHY" : api.getP95Ms() < 500 ? "WARNING" : "CRITICAL")
                .build();
    }

    @Override
    public KafkaMetricsResponse getKafkaMetrics() {
        try {
            Properties props = new Properties();
            props.put("bootstrap.servers", kafkaBootstrapServers);
            props.put("request.timeout.ms", 3000);
            try (AdminClient admin = AdminClient.create(props)) {
                var groups = admin.listConsumerGroups().all().get();
                return KafkaMetricsResponse.builder()
                        .connected(true)
                        .queueDepth(0) // Would need per-topic metrics
                        .consumerLag(0)
                        .activeConsumers(groups.size())
                        .totalPartitions(0)
                        .build();
            }
        } catch (Exception e) {
            log.warn("Kafka metrics unavailable: {}", e.getMessage());
            return KafkaMetricsResponse.builder().connected(false).build();
        }
    }

    @Override
    public RedisMetricsResponse getRedisMetrics() {
        try {
            Properties info = redisTemplate.execute((RedisConnection connection) ->
                    connection.serverCommands().info("stats"));
            if (info == null) {
                return RedisMetricsResponse.builder().connected(false).build();
            }
            long hits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
            long misses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
            long total = hits + misses;
            double hitRate = total > 0 ? (double) hits / total : 1.0;

            Properties keyspace = redisTemplate.execute((RedisConnection connection) ->
                    connection.serverCommands().info("keyspace"));
            long keys = 0;
            if (keyspace != null) {
                for (String key : keyspace.stringPropertyNames()) {
                    if (key.startsWith("db")) {
                        String val = keyspace.getProperty(key);
                        String[] parts = val.split(",");
                        for (String part : parts) {
                            if (part.startsWith("keys=")) {
                                keys += Long.parseLong(part.substring(5));
                            }
                        }
                    }
                }
            }

            Properties memory = redisTemplate.execute((RedisConnection connection) ->
                    connection.serverCommands().info("memory"));
            long usedMemory = 0;
            if (memory != null) {
                usedMemory = Long.parseLong(memory.getProperty("used_memory", "0"));
            }

            return RedisMetricsResponse.builder()
                    .connected(true)
                    .hitRate(hitRate)
                    .totalKeys(keys)
                    .evictions(Long.parseLong(info.getProperty("evicted_keys", "0")))
                    .usedMemoryBytes(usedMemory)
                    .build();
        } catch (Exception e) {
            log.warn("Redis metrics unavailable: {}", e.getMessage());
            return RedisMetricsResponse.builder().connected(false).build();
        }
    }

    @Override
    public ApiMetricsResponse getApiMetrics() {
        long[] percentiles = apiMetrics.getPercentiles();
        return ApiMetricsResponse.builder()
                .requestCount(apiMetrics.getRequestCount())
                .errorCount(apiMetrics.getErrorCount())
                .p50Ms(percentiles[0])
                .p95Ms(percentiles[1])
                .p99Ms(percentiles[2])
                .build();
    }
}
```

- [ ] **Step 7: Create MonitorController**

```java
package com.ticket.controller;

import com.ticket.common.constant.RoleConstants;
import com.ticket.dto.response.*;
import com.ticket.service.MonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/monitor")
@PreAuthorize("hasRole('" + RoleConstants.ADMIN + "')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Monitor", description = "Admin system monitoring endpoints")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get health overview for all monitored services")
    public ApiResult<MonitorOverviewResponse> getOverview() {
        return ApiResult.success(monitorService.getOverview());
    }

    @GetMapping("/kafka")
    @Operation(summary = "Get Kafka consumer group metrics")
    public ApiResult<KafkaMetricsResponse> getKafka() {
        return ApiResult.success(monitorService.getKafkaMetrics());
    }

    @GetMapping("/redis")
    @Operation(summary = "Get Redis cache metrics")
    public ApiResult<RedisMetricsResponse> getRedis() {
        return ApiResult.success(monitorService.getRedisMetrics());
    }

    @GetMapping("/api")
    @Operation(summary = "Get API latency percentiles")
    public ApiResult<ApiMetricsResponse> getApi() {
        return ApiResult.success(monitorService.getApiMetrics());
    }
}
```

- [ ] **Step 8: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/dto/response/MonitorOverviewResponse.java \
        ticket-system/backend/src/main/java/com/ticket/dto/response/KafkaMetricsResponse.java \
        ticket-system/backend/src/main/java/com/ticket/dto/response/RedisMetricsResponse.java \
        ticket-system/backend/src/main/java/com/ticket/dto/response/ApiMetricsResponse.java \
        ticket-system/backend/src/main/java/com/ticket/service/MonitorService.java \
        ticket-system/backend/src/main/java/com/ticket/service/impl/MonitorServiceImpl.java \
        ticket-system/backend/src/main/java/com/ticket/controller/MonitorController.java
git commit -m "feat: MonitorController — system health metrics for Kafka, Redis, API"
```

### Task 2.3: Monitor Backend Tests

- [ ] **Step 1: Create MonitorControllerTest** — skipped for brevity in plan, implement same pattern as ConfigControllerTest with `@WithMockUser(roles = "ADMIN")`

- [ ] **Step 2: Commit**

### Task 2.4: MonitorView.vue

**Files:**
- Create: `ticket-system/frontend/src/api/monitor.js`
- Create: `ticket-system/frontend/src/views/admin/MonitorView.vue`
- Modify: `ticket-system/frontend/src/router/index.js` (add route)
- Modify: `ticket-system/frontend/src/layouts/AppLayout.vue` (add nav link)

- [ ] **Step 1: Create monitor API**

```javascript
// ticket-system/frontend/src/api/monitor.js
import request from './request'

export function getMonitorOverviewApi() {
  return request.get('/admin/monitor/overview')
}

export function getKafkaMetricsApi() {
  return request.get('/admin/monitor/kafka')
}

export function getRedisMetricsApi() {
  return request.get('/admin/monitor/redis')
}

export function getApiMetricsApi() {
  return request.get('/admin/monitor/api')
}
```

- [ ] **Step 2: Create MonitorView.vue**

```vue
<template>
  <div class="monitor-page">
    <header class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">System Monitoring</h1>
        <p class="page-subtitle">Real-time infrastructure health</p>
      </div>
      <div class="page-header-right">
        <div class="poll-controls">
          <select v-model="pollInterval" class="poll-select" @change="restartPolling" aria-label="Polling interval">
            <option :value="10">10s</option>
            <option :value="30">30s</option>
            <option :value="60">60s</option>
            <option :value="120">120s</option>
          </select>
          <button class="pause-btn" @click="togglePause" :aria-label="paused ? 'Resume polling' : 'Pause polling'">
            {{ paused ? '▶' : '⏸' }}
          </button>
        </div>
      </div>
    </header>

    <!-- ── Status Cards ── -->
    <div class="status-cards">
      <div v-for="svc in services" :key="svc.key" class="status-card" :class="'status-card--' + svc.status">
        <span class="status-dot" :class="'status-dot--' + svc.status"></span>
        <div class="status-info">
          <span class="status-name">{{ svc.label }}</span>
          <span class="status-value">{{ svc.detail }}</span>
        </div>
        <span class="status-text">{{ statusLabel(svc.status) }}</span>
      </div>
    </div>

    <!-- ── Charts ── -->
    <div class="charts-grid">
      <!-- Kafka -->
      <div class="chart-card card">
        <div class="chart-card-header">
          <h3>Kafka Queue Depth</h3>
          <button class="text-btn" @click="toggleView('kafka')">{{ views.kafka === 'chart' ? 'Table' : 'Chart' }}</button>
        </div>
        <div v-if="views.kafka === 'chart'" ref="kafkaChart" class="chart-box"></div>
        <table v-else class="data-table">
          <thead><tr><th>Metric</th><th>Value</th></tr></thead>
          <tbody>
            <tr><td>Connected</td><td>{{ kafka.connected }}</td></tr>
            <tr><td>Active Consumers</td><td>{{ kafka.activeConsumers }}</td></tr>
          </tbody>
        </table>
      </div>

      <!-- Redis -->
      <div class="chart-card card">
        <div class="chart-card-header">
          <h3>Redis Cache Hit Rate</h3>
          <button class="text-btn" @click="toggleView('redis')">{{ views.redis === 'chart' ? 'Table' : 'Chart' }}</button>
        </div>
        <div v-if="views.redis === 'chart'" ref="redisChart" class="chart-box"></div>
        <table v-else class="data-table">
          <thead><tr><th>Metric</th><th>Value</th></tr></thead>
          <tbody>
            <tr><td>Hit Rate</td><td>{{ (redis.hitRate * 100).toFixed(1) }}%</td></tr>
            <tr><td>Total Keys</td><td>{{ redis.totalKeys.toLocaleString() }}</td></tr>
          </tbody>
        </table>
      </div>

      <!-- API -->
      <div class="chart-card card chart-card--wide">
        <div class="chart-card-header">
          <h3>API Response Time</h3>
          <button class="text-btn" @click="toggleView('api')">{{ views.api === 'chart' ? 'Table' : 'Chart' }}</button>
        </div>
        <div v-if="views.api === 'chart'" ref="apiChart" class="chart-box"></div>
        <table v-else class="data-table">
          <thead><tr><th>Percentile</th><th>Latency</th></tr></thead>
          <tbody>
            <tr><td>P50</td><td>{{ api.p50Ms }}ms</td></tr>
            <tr><td>P95</td><td>{{ api.p95Ms }}ms</td></tr>
            <tr><td>P99</td><td>{{ api.p99Ms }}ms</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getMonitorOverviewApi, getKafkaMetricsApi, getRedisMetricsApi, getApiMetricsApi } from '@/api/monitor'

const pollInterval = ref(parseInt(localStorage.getItem('monitorPollInterval') || '30'))
const paused = ref(false)
let pollTimer = null

const views = reactive({ kafka: 'chart', redis: 'chart', api: 'chart' })

const kafka = reactive({ connected: false, activeConsumers: 0, consumerLag: 0, queueDepth: 0 })
const redis = reactive({ connected: false, hitRate: 0, totalKeys: 0, evictions: 0, usedMemoryBytes: 0 })
const api = reactive({ requestCount: 0, errorCount: 0, p50Ms: 0, p95Ms: 0, p99Ms: 0 })
const services = ref([
  { key: 'kafka', label: 'Kafka', status: 'healthy', detail: 'Connected' },
  { key: 'redis', label: 'Redis', status: 'healthy', detail: '87.4% hit rate' },
  { key: 'api', label: 'API', status: 'healthy', detail: 'P95 128ms' }
])

const kafkaChart = ref(null)
const redisChart = ref(null)
const apiChart = ref(null)
let kafkaChartInst = null, redisChartInst = null, apiChartInst = null

onMounted(() => {
  fetchAll()
  startPolling()
})

onBeforeUnmount(() => {
  clearInterval(pollTimer)
  kafkaChartInst?.dispose()
  redisChartInst?.dispose()
  apiChartInst?.dispose()
})

function togglePause() {
  paused.value = !paused.value
  if (paused.value) {
    clearInterval(pollTimer)
  } else {
    startPolling()
  }
}

function startPolling() {
  clearInterval(pollTimer)
  if (!paused.value) {
    pollTimer = setInterval(fetchAll, pollInterval.value * 1000)
  }
}

function restartPolling() {
  localStorage.setItem('monitorPollInterval', pollInterval.value)
  if (!paused.value) startPolling()
}

async function fetchAll() {
  try {
    const [overviewRes, kafkaRes, redisRes, apiRes] = await Promise.all([
      getMonitorOverviewApi(), getKafkaMetricsApi(), getRedisMetricsApi(), getApiMetricsApi()
    ])
    if (overviewRes.data?.data) {
      const o = overviewRes.data.data
      updateServiceStatus('kafka', o.kafkaStatus); updateServiceStatus('redis', o.redisStatus); updateServiceStatus('api', o.apiStatus)
    }
    if (kafkaRes.data?.data) Object.assign(kafka, kafkaRes.data.data)
    if (redisRes.data?.data) Object.assign(redis, redisRes.data.data)
    if (apiRes.data?.data) Object.assign(api, apiRes.data.data)
    await nextTick(); renderCharts()
  } catch (e) { console.error('Monitor fetch error:', e) }
}

function updateServiceStatus(key, status) {
  const svc = services.value.find(s => s.key === key)
  if (svc) svc.status = status.toLowerCase()
}

function statusLabel(status) {
  if (status === 'healthy') return 'Healthy'
  if (status === 'warning') return 'Warning'
  return 'Critical'
}

function toggleView(chart) {
  views[chart] = views[chart] === 'chart' ? 'table' : 'chart'
}

function renderCharts() {
  if (kafkaChart.value && views.kafka === 'chart') {
    if (!kafkaChartInst) kafkaChartInst = echarts.init(kafkaChart.value)
    kafkaChartInst.setOption({ /* basic area chart */ })
  }
  // Redis and API chart rendering — implemented inline during execution
}
</script>

<style scoped>
.monitor-page { max-width: 1280px; margin: 0 auto; padding: var(--space-xl) var(--space-lg); }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: var(--space-lg); }
.page-header-left { display: flex; flex-direction: column; gap: var(--space-xs); }
.page-title { font-family: var(--font-heading); font-size: var(--text-2xl); font-weight: 700; color: var(--color-text-primary); margin: 0; }
.page-subtitle { font-size: var(--text-sm); color: var(--color-text-muted); margin: 0; }
.page-header-right { display: flex; align-items: center; gap: var(--space-sm); }
.poll-controls { display: flex; align-items: center; gap: var(--space-sm); }
.poll-select { padding: 8px 12px; font-size: var(--text-sm); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); background: var(--color-white); }
.pause-btn { display: flex; align-items: center; justify-content: center; width: 44px; height: 44px; padding: 0; font-size: 18px; background: var(--color-white); border: 1px solid var(--color-gray-200); border-radius: var(--radius-md); cursor: pointer; }

.status-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-md); margin-bottom: var(--space-lg); }
.status-card { display: flex; align-items: center; gap: var(--space-md); padding: var(--space-md) var(--space-lg); background: var(--color-white); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); border-left: 4px solid var(--color-gray-300); }
.status-card--healthy { border-left-color: #22C55E; }
.status-card--warning { border-left-color: #F59E0B; }
.status-card--critical { border-left-color: #EF4444; }
.status-dot { width: 12px; height: 12px; border-radius: 50%; background: var(--color-gray-300); flex-shrink: 0; }
.status-dot--healthy { background: #22C55E; }
.status-dot--warning { background: #F59E0B; }
.status-dot--critical { background: #EF4444; }
.status-info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.status-name { font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); }
.status-value { font-family: var(--font-mono); font-size: var(--text-xs); color: var(--color-text-muted); }
.status-text { font-family: var(--font-mono); font-size: var(--text-xs); font-weight: 600; color: inherit; }

.charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-md); }
.chart-card--wide { grid-column: 1 / -1; }
.chart-card-header { display: flex; align-items: center; justify-content: space-between; padding: var(--space-md) var(--space-lg); border-bottom: 1px solid var(--color-gray-100); }
.chart-card-header h3 { font-size: var(--text-sm); font-weight: 600; margin: 0; }
.chart-box { height: 260px; }
.text-btn { font-size: var(--text-xs); font-weight: 500; color: var(--color-primary); background: none; border: none; cursor: pointer; }
.data-table { width: 100%; padding: var(--space-md) var(--space-lg); }
.data-table td { padding: 6px 8px; font-size: var(--text-sm); }
.card { background: var(--color-white); border-radius: var(--radius-lg); box-shadow: var(--shadow-md); }

@media (max-width: 768px) {
  .status-cards { grid-template-columns: 1fr; }
  .charts-grid { grid-template-columns: 1fr; }
  .chart-card--wide { grid-column: auto; }
}
</style>
```

- [ ] **Step 3: Connect chart rendering** — The MonitorView template above includes chart rendering placeholders. During execution, the actual ECharts configuration (area chart for Kafka, bullet gauge for Redis, multi-line for API with 200ms threshold) will be implemented inline per the design spec.

- [ ] **Step 4: Add route** — In `router/index.js`, add route:

```javascript
      {
        path: 'admin/monitor',
        name: 'AdminMonitor',
        component: () => import('@/views/admin/MonitorView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
```

- [ ] **Step 5: Add nav link** — In `AppLayout.vue`, after Configuration link:

```html
          <router-link v-if="authStore.isAdmin" to="/admin/monitor" class="app-nav-link" :class="{ 'app-nav-link--active': $route.path.startsWith('/admin/monitor') }">
            Monitoring
          </router-link>
```

- [ ] **Step 6: Commit**

```bash
git add ticket-system/frontend/src/api/monitor.js \
        ticket-system/frontend/src/views/admin/MonitorView.vue \
        ticket-system/frontend/src/router/index.js \
        ticket-system/frontend/src/layouts/AppLayout.vue
git commit -m "feat: MonitorView — admin system health dashboard with ECharts"
```

---

## Feature 3: WebSocket Notifications

### Task 3.1: V10 Migration + Entity + Mapper

**Files:**
- Create: `ticket-system/backend/src/main/resources/db/migration/V10__create_notifications.sql`
- Create: `ticket-system/backend/src/main/java/com/ticket/entity/Notification.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/mapper/NotificationMapper.java`

- [ ] **Step 1: Write V10 migration**

```sql
-- V10: Notification table for WebSocket push
CREATE TABLE IF NOT EXISTS notification (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    type          VARCHAR(30) NOT NULL COMMENT 'TICKET_CREATED | TICKET_ASSIGNED | TICKET_REPLIED | TICKET_RESOLVED | TICKET_OVERDUE',
    ticket_id     BIGINT DEFAULT NULL,
    title         VARCHAR(255) NOT NULL,
    message       TEXT DEFAULT NULL,
    is_read       TINYINT NOT NULL DEFAULT 0,
    created_date  BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notif_user_read (user_id, is_read),
    KEY idx_notif_date (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 2: Create Notification entity**

```java
package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private Long ticketId;
    private String title;
    private String message;
    private Integer isRead;
    private Long createdDate;
}
```

- [ ] **Step 3: Create NotificationMapper**

```java
package com.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
```

- [ ] **Step 4: Commit**

### Task 3.2: WebSocket Stack

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/config/WebSocketConfig.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/security/WebSocketAuthInterceptor.java`
- Modify: `ticket-system/backend/src/main/java/com/ticket/security/SecurityConfig.java`

- [ ] **Step 1: Add dependency** — In `pom.xml`, add:

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
```

- [ ] **Step 2: Create WebSocketAuthInterceptor**

```java
package com.ticket.security;

import com.ticket.common.constant.ErrorCode;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Collections;

public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    private final JwtTokenProvider jwtTokenProvider;

    public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new org.springframework.messaging.simp.stomp.StompConversionException("Missing token");
            }
            String token = authHeader.substring(7);
            try {
                if (!jwtTokenProvider.validateToken(token)) {
                    throw new org.springframework.messaging.simp.stomp.StompConversionException("Invalid token");
                }
                Claims claims = jwtTokenProvider.parseToken(token);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                Principal principal = new UsernamePasswordAuthenticationToken(username, null, Collections.singletonList(authority));
                accessor.setUser(principal);
            } catch (Exception e) {
                log.warn("WebSocket auth failed: {}", e.getMessage());
                throw new org.springframework.messaging.simp.stomp.StompConversionException("Auth failed: " + e.getMessage());
            }
        }
        return message;
    }
}
```

- [ ] **Step 3: Create WebSocketConfig**

```java
package com.ticket.config;

import com.ticket.security.JwtTokenProvider;
import com.ticket.security.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;

    public WebSocketConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebSocketAuthInterceptor(jwtTokenProvider));
    }
}
```

- [ ] **Step 4: Allow /ws in SecurityConfig** — Add to `authorizeHttpRequests`:

```java
                .requestMatchers("/ws/**").permitAll()
```

- [ ] **Step 5: Commit**

### Task 3.3: NotificationService + Controller

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/event/NotificationService.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/response/NotificationResponse.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/controller/NotificationController.java`
- Modify: `ticket-system/backend/src/main/java/com/ticket/event/KafkaEventConsumer.java`

- [ ] **Step 1: Create NotificationResponse**

```java
package com.ticket.dto.response;

import lombok.Data;

@Data
public class NotificationResponse {
    private Long id;
    private String type;
    private Long ticketId;
    private String title;
    private String message;
    private Boolean isRead;
    private Long createdDate;
}
```

- [ ] **Step 2: Create NotificationService**

```java
package com.ticket.event;

import com.ticket.dto.response.NotificationResponse;
import com.ticket.entity.Notification;
import com.ticket.mapper.NotificationMapper;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedissonClient redissonClient;

    public NotificationService(NotificationMapper notificationMapper,
                               SimpMessagingTemplate messagingTemplate,
                               RedissonClient redissonClient) {
        this.notificationMapper = notificationMapper;
        this.messagingTemplate = messagingTemplate;
        this.redissonClient = redissonClient;
    }

    public void notifyUser(Long userId, String type, Long ticketId, String title, String message) {
        // Persist
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setType(type);
        notif.setTicketId(ticketId);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setIsRead(0);
        notif.setCreatedDate(System.currentTimeMillis());
        notificationMapper.insert(notif);

        // Increment Redis unread count
        RAtomicLong counter = redissonClient.getAtomicLong("unread:count:" + userId);
        counter.incrementAndGet();
        counter.expire(7, TimeUnit.DAYS);

        // Push via WebSocket
        NotificationResponse resp = toResponse(notif);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId), "/queue/notifications", resp);
    }

    public void notifyAllAgents(String topic, Object payload) {
        messagingTemplate.convertAndSend(topic, payload);
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId()); r.setType(n.getType()); r.setTicketId(n.getTicketId());
        r.setTitle(n.getTitle()); r.setMessage(n.getMessage());
        r.setIsRead(n.getIsRead() == 1); r.setCreatedDate(n.getCreatedDate());
        return r;
    }
}
```

- [ ] **Step 3: Create NotificationController**

```java
package com.ticket.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.constant.BusinessConstants;
import com.ticket.dto.response.*;
import com.ticket.entity.Notification;
import com.ticket.mapper.NotificationMapper;
import com.ticket.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationMapper notificationMapper;
    private final RedissonClient redissonClient;

    public NotificationController(NotificationMapper notificationMapper, RedissonClient redissonClient) {
        this.notificationMapper = notificationMapper;
        this.redissonClient = redissonClient;
    }

    @GetMapping
    @Operation(summary = "List notifications (paginated, newest first)")
    public ApiResult<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Notification> p = new Page<>(page, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, user.getUserId())
               .orderByDesc(Notification::getCreatedDate);
        notificationMapper.selectPage(p, wrapper);
        var records = p.getRecords().stream().map(this::toResponse).collect(Collectors.toList());
        return ApiResult.success(PageResponse.from(p, records));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ApiResult<Long> unreadCount(@AuthenticationPrincipal UserDetailsImpl user) {
        RAtomicLong counter = redissonClient.getAtomicLong("unread:count:" + user.getUserId());
        long count = counter.get();
        if (count == 0) {
            // Fallback to DB
            LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Notification::getUserId, user.getUserId())
                   .eq(Notification::getIsRead, 0);
            count = notificationMapper.selectCount(wrapper);
        }
        return ApiResult.success(count);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ApiResult<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl user) {
        Notification n = notificationMapper.selectById(id);
        if (n != null && n.getUserId().equals(user.getUserId())) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
            RAtomicLong counter = redissonClient.getAtomicLong("unread:count:" + user.getUserId());
            if (counter.get() > 0) counter.decrementAndGet();
        }
        return ApiResult.success();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ApiResult<Void> markAllRead(@AuthenticationPrincipal UserDetailsImpl user) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, user.getUserId())
               .eq(Notification::getIsRead, 0);
        Notification update = new Notification();
        update.setIsRead(1);
        notificationMapper.update(update, wrapper);
        redissonClient.getAtomicLong("unread:count:" + user.getUserId()).set(0);
        return ApiResult.success();
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId()); r.setType(n.getType()); r.setTicketId(n.getTicketId());
        r.setTitle(n.getTitle()); r.setMessage(n.getMessage());
        r.setIsRead(n.getIsRead() == 1); r.setCreatedDate(n.getCreatedDate());
        return r;
    }
}
```

- [ ] **Step 4: Wire Kafka consumer to NotificationService** — Update `KafkaEventConsumer.java` to inject `NotificationService`. For `ticket.created`, broadcast to `/topic/agents`. For `ticket.assigned`, notify assigned user.

- [ ] **Step 5: Commit**

### Task 3.4: Frontend — NotificationBell

**Files:**
- Create: `ticket-system/frontend/src/api/notifications.js`
- Create: `ticket-system/frontend/src/stores/notifications.js`
- Create: `ticket-system/frontend/src/components/NotificationBell.vue`
- Modify: `ticket-system/frontend/src/layouts/AppLayout.vue`

- [ ] **Step 1: Create notifications API**

```javascript
// ticket-system/frontend/src/api/notifications.js
import request from './request'

export function getNotificationsApi(params) {
  return request.get('/notifications', { params })
}

export function getUnreadCountApi() {
  return request.get('/notifications/unread-count')
}

export function markReadApi(id) {
  return request.put(`/notifications/${id}/read`)
}

export function markAllReadApi() {
  return request.put('/notifications/read-all')
}
```

- [ ] **Step 2: Create notifications store** — (Full Pinia store with STOMP connect/disconnect, unreadCount, items)

- [ ] **Step 3: Create NotificationBell.vue** — (Bell icon + red badge + Popover with notification list, "Mark all read", relative timestamps)

- [ ] **Step 4: Integrate into AppLayout** — Mount `<NotificationBell />` in navbar before `<button class="app-logout-btn">`

- [ ] **Step 5: Install npm packages and commit**

```bash
cd ticket-system/frontend && npm install @stomp/stompjs sockjs-client
git add ticket-system/frontend/src/api/notifications.js \
        ticket-system/frontend/src/stores/notifications.js \
        ticket-system/frontend/src/components/NotificationBell.vue \
        ticket-system/frontend/src/layouts/AppLayout.vue \
        ticket-system/frontend/package.json ticket-system/frontend/package-lock.json
git commit -m "feat: WebSocket notifications — NotificationBell with STOMP"
```

---

## Feature 4: Timeout Alerts

### Task 4.1: TicketOverdueEvent + Kafka Producer

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/event/TicketOverdueEvent.java`
- Modify: `ticket-system/backend/src/main/java/com/ticket/event/EventPublisher.java`
- Modify: `ticket-system/backend/src/main/java/com/ticket/event/KafkaEventPublisher.java`

- [ ] **Step 1: Create TicketOverdueEvent**

```java
package com.ticket.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketOverdueEvent {
    private Long ticketId;
    private String ticketTitle;
    private String priority;
    private String overdueType; // UNASSIGNED_OVERDUE | RESOLUTION_OVERDUE
    private Long overdueHours;
    private Long assignedTo; // null if unassigned
    private Long timestamp;
}
```

- [ ] **Step 2: Add to EventPublisher interface**

```java
    void publishTicketOverdue(TicketOverdueEvent event);
```

- [ ] **Step 3: Add to KafkaEventPublisher**

```java
    public static final String TOPIC_TICKET_OVERDUE = "ticket.overdue";

    @Override
    public void publishTicketOverdue(TicketOverdueEvent event) {
        send(TOPIC_TICKET_OVERDUE, String.valueOf(event.getTicketId()), event);
    }
```

- [ ] **Step 4: Commit**

### Task 4.2: SlaCheckScheduler

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/task/SlaCheckScheduler.java`
- Create: `ticket-system/backend/src/test/java/com/ticket/task/SlaCheckSchedulerTest.java`

- [ ] **Step 1: Create SlaCheckScheduler**

```java
package com.ticket.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.common.constant.BusinessConstants;
import com.ticket.entity.*;
import com.ticket.event.EventPublisher;
import com.ticket.event.TicketOverdueEvent;
import com.ticket.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@EnableScheduling
public class SlaCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaCheckScheduler.class);

    private final TicketMapper ticketMapper;
    private final SlaConfigMapper slaMapper;
    private final EventPublisher eventPublisher;

    public SlaCheckScheduler(TicketMapper ticketMapper, SlaConfigMapper slaMapper,
                             EventPublisher eventPublisher) {
        this.ticketMapper = ticketMapper;
        this.slaMapper = slaMapper;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 300_000) // Every 5 minutes
    public void detectOverdueTickets() {
        log.info("SLA check started");
        long now = System.currentTimeMillis();

        // Load SLA config — build priority → hours map
        List<SlaConfig> slas = slaMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, Integer[]> slaMap = new HashMap<>(); // priority → [response, resolution]
        for (SlaConfig sla : slas) {
            slaMap.put(sla.getPriority(), new Integer[]{sla.getResponseHours(), sla.getResolutionHours()});
        }

        // 1. Unassigned overdue (OPEN, no assignee)
        LambdaQueryWrapper<Ticket> unassignedQ = new LambdaQueryWrapper<>();
        unassignedQ.eq(Ticket::getStatus, BusinessConstants.TICKET_STATUS_OPEN)
                    .isNull(Ticket::getAssignedTo);
        List<Ticket> unassigned = ticketMapper.selectList(unassignedQ);

        for (Ticket t : unassigned) {
            Integer[] hours = slaMap.get(t.getPriority());
            if (hours == null) continue;
            int responseHours = hours[0];
            long dueTime = t.getCreatedDate() + (long) responseHours * 3600_000L;
            if (now > dueTime) {
                long overdueH = (now - dueTime) / 3600_000L;
                eventPublisher.publishTicketOverdue(new TicketOverdueEvent(
                        t.getId(), t.getTitle(), t.getPriority(),
                        "UNASSIGNED_OVERDUE", overdueH, null, now));
            }
        }

        // 2. Resolution overdue (OPEN or IN_PROGRESS, has assignee)
        LambdaQueryWrapper<Ticket> unresolvedQ = new LambdaQueryWrapper<>();
        unresolvedQ.in(Ticket::getStatus, BusinessConstants.TICKET_STATUS_OPEN, BusinessConstants.TICKET_STATUS_IN_PROGRESS)
                     .isNotNull(Ticket::getAssignedTo);
        List<Ticket> unresolved = ticketMapper.selectList(unresolvedQ);

        for (Ticket t : unresolved) {
            Integer[] hours = slaMap.get(t.getPriority());
            if (hours == null) continue;
            int resolutionHours = hours[1];
            long dueTime = t.getCreatedDate() + (long) resolutionHours * 3600_000L;
            if (now > dueTime) {
                long overdueH = (now - dueTime) / 3600_000L;
                eventPublisher.publishTicketOverdue(new TicketOverdueEvent(
                        t.getId(), t.getTitle(), t.getPriority(),
                        "RESOLUTION_OVERDUE", overdueH, t.getAssignedTo(), now));
            }
        }
        log.info("SLA check completed — unassigned checked: {}, unresolved checked: {}", unassigned.size(), unresolved.size());
    }
}
```

- [ ] **Step 2: Create SlaCheckSchedulerTest**

```java
package com.ticket.task;

import com.ticket.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestConfig.class)
@Sql(scripts = "/sql/init-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class SlaCheckSchedulerTest {

    @Autowired
    private SlaCheckScheduler scheduler;

    @Test
    void shouldExecuteWithoutExceptions() {
        assertDoesNotThrow(() -> scheduler.detectOverdueTickets());
    }
}
```

- [ ] **Step 3: Commit**

### Task 4.3: Frontend — AgentWorkbench Overdue Section

**Files:**
- Modify: `ticket-system/frontend/src/views/AgentWorkbench.vue`

- [ ] **Step 1: Add overdue banner and badge** — Add collapsible top banner with "⚠ Overdue Tickets" count, and orange left border + ⏰ icon on overdue rows in queue.

- [ ] **Step 2: Commit**

---

## Post-Implementation Checklist

- [ ] Run all backend tests: `cd ticket-system/backend && mvn test`
- [ ] Run frontend build: `cd ticket-system/frontend && npm run build`
- [ ] Verify new admin nav links visible when logged in as ADMIN
- [ ] Create custom field → appears in config page → can be deleted
- [ ] Edit SLA rule → confirmation dialog → value persists
- [ ] Monitor page shows live data (check with dev services running)
- [ ] Pause/Resume polling works
- [ ] WebSocket connects on page load, bell shows unread count
- [ ] Notification popover opens, mark read works
- [ ] SLA scheduler logs overdue tickets (check server logs)

---

## Remaining Task Dependency Setup

