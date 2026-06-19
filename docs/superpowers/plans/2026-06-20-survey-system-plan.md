# Survey System — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an enterprise survey system with hierarchical template builder (Page→Section→Question), conditional visibility rules, multi-page filling, and result analytics.

**Architecture:** Follows existing ticket-system patterns: DB migration → Entity → Mapper → DTO → Service → Controller (Spring Boot 3.2 + MyBatis-Plus 3.5). Frontend uses Vue 3 + Element Plus with custom CSS variables. Visibility engine uses dependency graph for incremental recalculation.

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus 3.5, Vue 3.4, Element Plus 2.x, Pinia 2.x, Kafka (notifications)

**Prerequisite:** Multi-tenancy v1.1 (all survey tables created with `tenant_id` after tenant infrastructure is in place)

---

## File Map

### Backend (Java)

| Action | File | Purpose |
|--------|------|---------|
| Create | `src/main/resources/db/migration/V11__create_survey_tables.sql` | 7 tables |
| Create | `src/main/java/com/ticket/entity/SurveyTemplate.java` | Template entity |
| Create | `src/main/java/com/ticket/entity/SurveyPage.java` | Page entity |
| Create | `src/main/java/com/ticket/entity/SurveySection.java` | Section entity |
| Create | `src/main/java/com/ticket/entity/SurveyQuestion.java` | Question entity |
| Create | `src/main/java/com/ticket/entity/SurveyVisibilityRule.java` | Visibility rule entity |
| Create | `src/main/java/com/ticket/entity/SurveyInstance.java` | Instance entity |
| Create | `src/main/java/com/ticket/entity/SurveyInstancePage.java` | Instance-page state entity |
| Create | `src/main/java/com/ticket/entity/SurveyAnswer.java` | Answer entity |
| Create | `src/main/java/com/ticket/mapper/SurveyTemplateMapper.java` | Mapper |
| Create | `src/main/java/com/ticket/mapper/SurveyPageMapper.java` | Mapper |
| Create | `src/main/java/com/ticket/mapper/SurveySectionMapper.java` | Mapper |
| Create | `src/main/java/com/ticket/mapper/SurveyQuestionMapper.java` | Mapper |
| Create | `src/main/java/com/ticket/mapper/SurveyVisibilityRuleMapper.java` | Mapper |
| Create | `src/main/java/com/ticket/mapper/SurveyInstanceMapper.java` | Mapper |
| Create | `src/main/java/com/ticket/mapper/SurveyInstancePageMapper.java` | Mapper |
| Create | `src/main/java/com/ticket/mapper/SurveyAnswerMapper.java` | Mapper |
| Create | `src/main/java/com/ticket/dto/request/CreateSurveyTemplateRequest.java` | DTO |
| Create | `src/main/java/com/ticket/dto/request/UpdateSurveyTemplateRequest.java` | DTO |
| Create | `src/main/java/com/ticket/dto/request/CreateSurveyInstanceRequest.java` | DTO |
| Create | `src/main/java/com/ticket/dto/request/SaveAnswerRequest.java` | DTO |
| Create | `src/main/java/com/ticket/dto/request/SubmitSurveyRequest.java` | DTO |
| Create | `src/main/java/com/ticket/dto/response/SurveyTemplateResponse.java` | DTO |
| Create | `src/main/java/com/ticket/dto/response/SurveyInstanceResponse.java` | DTO |
| Create | `src/main/java/com/ticket/dto/response/SurveyFillResponse.java` | DTO |
| Create | `src/main/java/com/ticket/dto/response/SurveyResultResponse.java` | DTO |
| Create | `src/main/java/com/ticket/service/SurveyService.java` | Interface |
| Create | `src/main/java/com/ticket/service/impl/SurveyServiceImpl.java` | Implementation |
| Create | `src/main/java/com/ticket/service/SurveyVisibilityEngine.java` | Visibility engine |
| Create | `src/main/java/com/ticket/controller/SurveyController.java` | Admin endpoints |
| Create | `src/main/java/com/ticket/controller/SurveyFillController.java` | User fill endpoints |
| Modify | `src/main/java/com/ticket/common/constant/ErrorCode.java` | Add survey error codes |
| Modify | `src/main/java/com/ticket/common/constant/BusinessConstants.java` | Add survey constants |
| Modify | `src/main/java/com/ticket/security/SecurityConfig.java` | Add survey paths |

### Frontend (Vue 3)

| Action | File | Purpose |
|--------|------|---------|
| Create | `src/api/survey.js` | API layer |
| Create | `src/stores/survey.js` | Pinia store |
| Create | `src/views/admin/SurveyListView.vue` | Template management list |
| Create | `src/views/admin/SurveyBuilderView.vue` | 3-column template builder |
| Create | `src/views/admin/SurveyResultsView.vue` | Results analytics |
| Create | `src/views/SurveyFillView.vue` | Multi-page fill page |
| Create | `src/components/survey/SurveyLeftNav.vue` | Page navigation sidebar |
| Create | `src/components/survey/SurveyQuestionRenderer.vue` | Question type dispatcher |
| Create | `src/components/survey/RuleEditorModal.vue` | Visibility rule editor modal |
| Modify | `src/router/index.js` | Add survey routes |
| Modify | `src/layouts/AppLayout.vue` | Add survey nav link |
| Modify | `src/views/DashboardPlaceholder.vue` | Add pending surveys section |

---

## Phase 1: Data Layer

### Task 1.1: DB Migration

**Files:**
- Create: `ticket-system/backend/src/main/resources/db/migration/V11__create_survey_tables.sql`

- [ ] **Step 1: Write migration SQL**

```sql
-- V11: Survey system — templates, questions, instances, answers
CREATE TABLE IF NOT EXISTS survey_template (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    title               VARCHAR(255) NOT NULL,
    description         TEXT DEFAULT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    version             INT NOT NULL DEFAULT 1,
    origin_id           BIGINT DEFAULT NULL,
    allow_resubmit      TINYINT NOT NULL DEFAULT 0,
    created_by          BIGINT NOT NULL,
    created_date        BIGINT NOT NULL,
    last_modified_by    BIGINT DEFAULT NULL,
    last_modified_date  BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_st_template_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS survey_page (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    template_id         BIGINT NOT NULL,
    title               VARCHAR(255) NOT NULL,
    display_order       INT NOT NULL DEFAULT 0,
    created_by          BIGINT NOT NULL,
    created_date        BIGINT NOT NULL,
    last_modified_by    BIGINT DEFAULT NULL,
    last_modified_date  BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (template_id) REFERENCES survey_template(id) ON DELETE CASCADE,
    INDEX idx_sp_template (template_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS survey_section (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    page_id             BIGINT NOT NULL,
    title               VARCHAR(255) NOT NULL,
    description         TEXT DEFAULT NULL,
    display_order       INT NOT NULL DEFAULT 0,
    created_by          BIGINT NOT NULL,
    created_date        BIGINT NOT NULL,
    last_modified_by    BIGINT DEFAULT NULL,
    last_modified_date  BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (page_id) REFERENCES survey_page(id) ON DELETE CASCADE,
    INDEX idx_ss_page (page_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS survey_question (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    section_id          BIGINT NOT NULL,
    type                VARCHAR(20) NOT NULL,
    title               VARCHAR(500) NOT NULL,
    description         TEXT DEFAULT NULL,
    options             JSON DEFAULT NULL,
    required            TINYINT NOT NULL DEFAULT 0,
    display_order       INT NOT NULL DEFAULT 0,
    created_by          BIGINT NOT NULL,
    created_date        BIGINT NOT NULL,
    last_modified_by    BIGINT DEFAULT NULL,
    last_modified_date  BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (section_id) REFERENCES survey_section(id) ON DELETE CASCADE,
    INDEX idx_sq_section (section_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS survey_visibility_rule (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    template_id         BIGINT NOT NULL,
    target_type         VARCHAR(10) NOT NULL,
    target_id           BIGINT NOT NULL,
    source_question_id  BIGINT NOT NULL,
    op                  VARCHAR(15) NOT NULL,
    value               VARCHAR(500) DEFAULT NULL,
    logic_group         INT NOT NULL DEFAULT 0,
    display_order       INT NOT NULL DEFAULT 0,
    created_by          BIGINT NOT NULL,
    created_date        BIGINT NOT NULL,
    last_modified_by    BIGINT DEFAULT NULL,
    last_modified_date  BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_svr_target (template_id, target_type, target_id),
    INDEX idx_svr_source (source_question_id),
    FOREIGN KEY (source_question_id) REFERENCES survey_question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS survey_instance (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    template_id         BIGINT NOT NULL,
    title               VARCHAR(255) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'READY_TO_START',
    assigned_to         BIGINT NOT NULL,
    trigger_type        VARCHAR(20) NOT NULL,
    ticket_id           BIGINT DEFAULT NULL,
    created_by          BIGINT NOT NULL,
    created_date        BIGINT NOT NULL,
    last_modified_by    BIGINT DEFAULT NULL,
    last_modified_date  BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (template_id) REFERENCES survey_template(id),
    INDEX idx_si_user (assigned_to),
    INDEX idx_si_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS survey_instance_page (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    instance_id     BIGINT NOT NULL,
    page_id         BIGINT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'READY_TO_START',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sip (instance_id, page_id),
    FOREIGN KEY (instance_id) REFERENCES survey_instance(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS survey_answer (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    instance_id     BIGINT NOT NULL,
    question_id     BIGINT NOT NULL,
    value           JSON NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sa (instance_id, question_id),
    FOREIGN KEY (instance_id) REFERENCES survey_instance(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES survey_question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 2: Commit**

```bash
git add ticket-system/backend/src/main/resources/db/migration/V11__create_survey_tables.sql
git commit -m "feat: V11 migration — survey system 7 tables"
```

### Task 1.2: Entities

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/entity/SurveyTemplate.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/entity/SurveyPage.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/entity/SurveySection.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/entity/SurveyQuestion.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/entity/SurveyVisibilityRule.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/entity/SurveyInstance.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/entity/SurveyInstancePage.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/entity/SurveyAnswer.java`

- [ ] **Step 1: Create SurveyTemplate**

```java
package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_template")
public class SurveyTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String title;
    private String description;
    private String status;
    private Integer version;
    private Long originId;
    private Integer allowResubmit;

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

- [ ] **Step 2: Create SurveyPage**

```java
package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_page")
public class SurveyPage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private String title;
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

- [ ] **Step 3: Create SurveySection**

```java
package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_section")
public class SurveySection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pageId;
    private String title;
    private String description;
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

- [ ] **Step 4: Create SurveyQuestion**

```java
package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_question")
public class SurveyQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sectionId;
    private String type;
    private String title;
    private String description;
    private String options; // JSON string
    private Integer required;
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

- [ ] **Step 5: Create SurveyVisibilityRule**

```java
package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_visibility_rule")
public class SurveyVisibilityRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private String targetType;
    private Long targetId;
    private Long sourceQuestionId;
    private String op;
    private String value;
    private Integer logicGroup;
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

- [ ] **Step 6: Create SurveyInstance**

```java
package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_instance")
public class SurveyInstance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long templateId;
    private String title;
    private String status;
    private Long assignedTo;
    private String triggerType;
    private Long ticketId;

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

- [ ] **Step 7: Create SurveyInstancePage**

```java
package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_instance_page")
public class SurveyInstancePage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Long pageId;
    private String status;
}
```

- [ ] **Step 8: Create SurveyAnswer**

```java
package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("survey_answer")
public class SurveyAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Long questionId;
    private String value; // JSON string
}
```

- [ ] **Step 9: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/entity/SurveyTemplate.java \
        ticket-system/backend/src/main/java/com/ticket/entity/SurveyPage.java \
        ticket-system/backend/src/main/java/com/ticket/entity/SurveySection.java \
        ticket-system/backend/src/main/java/com/ticket/entity/SurveyQuestion.java \
        ticket-system/backend/src/main/java/com/ticket/entity/SurveyVisibilityRule.java \
        ticket-system/backend/src/main/java/com/ticket/entity/SurveyInstance.java \
        ticket-system/backend/src/main/java/com/ticket/entity/SurveyInstancePage.java \
        ticket-system/backend/src/main/java/com/ticket/entity/SurveyAnswer.java
git commit -m "feat: survey entities — 8 MyBatis-Plus entities"
```

### Task 1.3: Mappers

**Files:**
- Create: 8 mapper interfaces

- [ ] **Step 1: Create all mappers**

```java
// SurveyTemplateMapper.java
package com.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.entity.SurveyTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SurveyTemplateMapper extends BaseMapper<SurveyTemplate> {}
```

Same pattern for: SurveyPageMapper, SurveySectionMapper, SurveyQuestionMapper, SurveyVisibilityRuleMapper, SurveyInstanceMapper, SurveyInstancePageMapper, SurveyAnswerMapper.

- [ ] **Step 2: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/mapper/SurveyTemplateMapper.java \
        ticket-system/backend/src/main/java/com/ticket/mapper/SurveyPageMapper.java \
        ticket-system/backend/src/main/java/com/ticket/mapper/SurveySectionMapper.java \
        ticket-system/backend/src/main/java/com/ticket/mapper/SurveyQuestionMapper.java \
        ticket-system/backend/src/main/java/com/ticket/mapper/SurveyVisibilityRuleMapper.java \
        ticket-system/backend/src/main/java/com/ticket/mapper/SurveyInstanceMapper.java \
        ticket-system/backend/src/main/java/com/ticket/mapper/SurveyInstancePageMapper.java \
        ticket-system/backend/src/main/java/com/ticket/mapper/SurveyAnswerMapper.java
git commit -m "feat: survey mappers — 8 BaseMapper interfaces"
```

### Task 1.4: Error Codes + Constants

**Files:**
- Modify: `ticket-system/backend/src/main/java/com/ticket/common/constant/ErrorCode.java`
- Modify: `ticket-system/backend/src/main/java/com/ticket/common/constant/BusinessConstants.java`

- [ ] **Step 1: Add survey error codes**

```java
// In ErrorCode.java, add after existing survey codes:
    TEMPLATE_NOT_FOUND(40030, "survey template not found"),
    TEMPLATE_ALREADY_PUBLISHED(40031, "published templates cannot be deleted"),
    TEMPLATE_VERSION_CONFLICT(40032, "only one published version allowed per template chain"),
    INSTANCE_NOT_FOUND(40033, "survey instance not found"),
    INSTANCE_ALREADY_SUBMITTED(40034, "survey instance already submitted"),
    SURVEY_PAGE_INCOMPLETE(40035, "please complete all required questions on this page"),
    SURVEY_CYCLE_DETECTED(40036, "visibility rules contain a cycle — please adjust"),
```

- [ ] **Step 2: Add survey constants**

```java
// In BusinessConstants.java, add:
    // === Survey status ===
    public static final String SURVEY_STATUS_DRAFT = "DRAFT";
    public static final String SURVEY_STATUS_PUBLISHED = "PUBLISHED";
    public static final String SURVEY_STATUS_ARCHIVED = "ARCHIVED";

    // === Survey instance status ===
    public static final String INSTANCE_STATUS_READY = "READY_TO_START";
    public static final String INSTANCE_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String INSTANCE_STATUS_SUBMITTED = "SUBMITTED";
    public static final String INSTANCE_STATUS_COMPLETED = "COMPLETED";
    public static final String INSTANCE_STATUS_REOPENED = "REOPENED";

    // === Survey instance page status ===
    public static final String SURVEY_PAGE_READY = "READY_TO_START";
    public static final String SURVEY_PAGE_IN_PROGRESS = "IN_PROGRESS";
    public static final String SURVEY_PAGE_COMPLETE = "COMPLETE";

    // === Survey trigger types ===
    public static final String SURVEY_TRIGGER_TICKET = "TICKET";
    public static final String SURVEY_TRIGGER_MANUAL = "MANUAL";

    // === Survey visibility target types ===
    public static final String SURVEY_TARGET_PAGE = "PAGE";
    public static final String SURVEY_TARGET_SECTION = "SECTION";
    public static final String SURVEY_TARGET_QUESTION = "QUESTION";

    // === Survey question types ===
    public static final String QTYPE_SINGLE_CHOICE = "SINGLE_CHOICE";
    public static final String QTYPE_MULTI_CHOICE = "MULTI_CHOICE";
    public static final String QTYPE_TEXT = "TEXT";
    public static final String QTYPE_TEXTAREA = "TEXTAREA";
    public static final String QTYPE_DATE = "DATE";
    public static final String QTYPE_DROPDOWN = "DROPDOWN";
    public static final String QTYPE_CASCADER = "CASCADER";
    public static final String QTYPE_RATING = "RATING";
    public static final String QTYPE_TABLE = "TABLE";
```

- [ ] **Step 3: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/common/constant/ErrorCode.java \
        ticket-system/backend/src/main/java/com/ticket/common/constant/BusinessConstants.java
git commit -m "feat: survey error codes + business constants"
```

---

## Phase 2: Template CRUD + Builder (Backend)

### Task 2.1: Template DTOs

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/request/CreateSurveyTemplateRequest.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/response/SurveyTemplateResponse.java`

- [ ] **Step 1: Create request DTO**

```java
package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSurveyTemplateRequest {
    @NotBlank(message = "title is required")
    @Size(max = 255)
    private String title;

    private String description;
    private Integer allowResubmit = 0;
}
```

- [ ] **Step 2: Create response DTO with nested structure**

```java
package com.ticket.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class SurveyTemplateResponse {
    private Long id;
    private String title;
    private String description;
    private String status;
    private Integer version;
    private Long originId;
    private Integer allowResubmit;
    private Long createdDate;
    private List<PageResponse> pages;

    @Data
    public static class PageResponse {
        private Long id;
        private String title;
        private Integer displayOrder;
        private List<SectionResponse> sections;
    }

    @Data
    public static class SectionResponse {
        private Long id;
        private String title;
        private String description;
        private Integer displayOrder;
        private List<QuestionResponse> questions;
    }

    @Data
    public static class QuestionResponse {
        private Long id;
        private String type;
        private String title;
        private String description;
        private String options;
        private Boolean required;
        private Integer displayOrder;
        private List<VisibilityRuleResponse> visibilityRules;
    }

    @Data
    public static class VisibilityRuleResponse {
        private Long id;
        private Long sourceQuestionId;
        private String op;
        private String value;
        private Integer logicGroup;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/dto/request/CreateSurveyTemplateRequest.java \
        ticket-system/backend/src/main/java/com/ticket/dto/response/SurveyTemplateResponse.java
git commit -m "feat: survey template DTOs — request + nested response"
```

### Task 2.2: SurveyService — Template CRUD

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/service/SurveyService.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/service/impl/SurveyServiceImpl.java`

- [ ] **Step 1: Create service interface**

```java
package com.ticket.service;

import com.ticket.dto.request.*;
import com.ticket.dto.response.*;

import java.util.List;

public interface SurveyService {
    // Template CRUD
    SurveyTemplateResponse createTemplate(CreateSurveyTemplateRequest request, Long adminId);
    SurveyTemplateResponse getTemplate(Long templateId, Long userId, String role);
    List<SurveyTemplateResponse> listTemplates();
    SurveyTemplateResponse updateTemplateTitle(Long id, UpdateSurveyTemplateRequest request, Long adminId);
    void deleteTemplate(Long id);
}
```

- [ ] **Step 2: Create service implementation**

```java
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public SurveyTemplateResponse createTemplate(CreateSurveyTemplateRequest request, Long adminId) {
        SurveyTemplate t = new SurveyTemplate();
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

        log.info("Survey template created: id={} title={}", t.getId(), t.getTitle());
        return toTemplateResponse(t);
    }

    @Override
    public SurveyTemplateResponse getTemplate(Long templateId, Long userId, String role) {
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
    public SurveyTemplateResponse updateTemplateTitle(Long id, UpdateSurveyTemplateRequest request, Long adminId) {
        SurveyTemplate t = findTemplateOrFail(id);
        if (request.getTitle() != null) t.setTitle(request.getTitle());
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
        templateMapper.deleteById(id);
    }

    private SurveyTemplate findTemplateOrFail(Long id) {
        SurveyTemplate t = templateMapper.selectById(id);
        if (t == null) throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND);
        return t;
    }

    private SurveyTemplateResponse toTemplateResponse(SurveyTemplate t) {
        SurveyTemplateResponse r = new SurveyTemplateResponse();
        r.setId(t.getId());
        r.setTitle(t.getTitle());
        r.setDescription(t.getDescription());
        r.setStatus(t.getStatus());
        r.setVersion(t.getVersion());
        r.setOriginId(t.getOriginId());
        r.setAllowResubmit(t.getAllowResubmit() == 1);
        r.setCreatedDate(t.getCreatedDate());

        // Load nested structure
        List<SurveyPage> pages = pageMapper.selectList(
                new LambdaQueryWrapper<SurveyPage>()
                        .eq(SurveyPage::getTemplateId, t.getId())
                        .orderByAsc(SurveyPage::getDisplayOrder));
        r.setPages(pages.stream().map(p -> {
            SurveyTemplateResponse.PageResponse pr = new SurveyTemplateResponse.PageResponse();
            pr.setId(p.getId());
            pr.setTitle(p.getTitle());
            pr.setDisplayOrder(p.getDisplayOrder());

            List<SurveySection> sections = sectionMapper.selectList(
                    new LambdaQueryWrapper<SurveySection>()
                            .eq(SurveySection::getPageId, p.getId())
                            .orderByAsc(SurveySection::getDisplayOrder));
            pr.setSections(sections.stream().map(s -> {
                SurveyTemplateResponse.SectionResponse sr = new SurveyTemplateResponse.SectionResponse();
                sr.setId(s.getId());
                sr.setTitle(s.getTitle());
                sr.setDescription(s.getDescription());
                sr.setDisplayOrder(s.getDisplayOrder());

                List<SurveyQuestion> questions = questionMapper.selectList(
                        new LambdaQueryWrapper<SurveyQuestion>()
                                .eq(SurveyQuestion::getSectionId, s.getId())
                                .orderByAsc(SurveyQuestion::getDisplayOrder));
                sr.setQuestions(questions.stream().map(q -> {
                    SurveyTemplateResponse.QuestionResponse qr = new SurveyTemplateResponse.QuestionResponse();
                    qr.setId(q.getId());
                    qr.setType(q.getType());
                    qr.setTitle(q.getTitle());
                    qr.setDescription(q.getDescription());
                    qr.setOptions(q.getOptions());
                    qr.setRequired(q.getRequired() == 1);
                    qr.setDisplayOrder(q.getDisplayOrder());

                    List<SurveyVisibilityRule> rules = ruleMapper.selectList(
                            new LambdaQueryWrapper<SurveyVisibilityRule>()
                                    .eq(SurveyVisibilityRule::getTargetType, BusinessConstants.SURVEY_TARGET_QUESTION)
                                    .eq(SurveyVisibilityRule::getTargetId, q.getId())
                                    .orderByAsc(SurveyVisibilityRule::getLogicGroup, SurveyVisibilityRule::getDisplayOrder));
                    qr.setVisibilityRules(rules.stream().map(rl -> {
                        SurveyTemplateResponse.VisibilityRuleResponse vr = new SurveyTemplateResponse.VisibilityRuleResponse();
                        vr.setId(rl.getId());
                        vr.setSourceQuestionId(rl.getSourceQuestionId());
                        vr.setOp(rl.getOp());
                        vr.setValue(rl.getValue());
                        vr.setLogicGroup(rl.getLogicGroup());
                        return vr;
                    }).collect(Collectors.toList()));

                    return qr;
                }).collect(Collectors.toList()));
                return sr;
            }).collect(Collectors.toList()));
            return pr;
        }).collect(Collectors.toList()));

        return r;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/service/SurveyService.java \
        ticket-system/backend/src/main/java/com/ticket/service/impl/SurveyServiceImpl.java
git commit -m "feat: SurveyService — template CRUD with nested structure loading"
```

### Task 2.3: SurveyController — Template Endpoints

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/controller/SurveyController.java`

- [ ] **Step 1: Create controller**

```java
package com.ticket.controller;

import com.ticket.common.constant.RoleConstants;
import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.SurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/surveys")
@PreAuthorize("hasRole('" + RoleConstants.ADMIN + "')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Survey Admin", description = "Admin survey template management")
public class SurveyController {

    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @GetMapping
    @Operation(summary = "List all survey templates")
    public ApiResult<List<SurveyTemplateResponse>> list() {
        return ApiResult.success(surveyService.listTemplates());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template with full nested structure")
    public ApiResult<SurveyTemplateResponse> get(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.getTemplate(id, user.getUserId(), user.getRole()));
    }

    @PostMapping
    @Operation(summary = "Create a new survey template")
    public ApiResult<SurveyTemplateResponse> create(@Valid @RequestBody CreateSurveyTemplateRequest request,
                                                     @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.createTemplate(request, user.getUserId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update template title and description")
    public ApiResult<SurveyTemplateResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateSurveyTemplateRequest request,
                                                     @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.updateTemplateTitle(id, request, user.getUserId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a DRAFT template")
    public ApiResult<Void> delete(@PathVariable Long id) {
        surveyService.deleteTemplate(id);
        return ApiResult.success();
    }
}
```

- [ ] **Step 2: Add paths to SecurityConfig**

```java
// In SecurityConfig.java, add:
.requestMatchers("/api/admin/surveys/**").hasRole(RoleConstants.ADMIN)
.requestMatchers("/api/surveys/**").authenticated()
```

- [ ] **Step 3: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/controller/SurveyController.java \
        ticket-system/backend/src/main/java/com/ticket/security/SecurityConfig.java
git commit -m "feat: SurveyController — admin template CRUD endpoints"
```

### Task 2.4: Page/Section/Question Builder Endpoints

**Files:**
- Modify: `ticket-system/backend/src/main/java/com/ticket/service/SurveyService.java`
- Modify: `ticket-system/backend/src/main/java/com/ticket/service/impl/SurveyServiceImpl.java`
- Modify: `ticket-system/backend/src/main/java/com/ticket/controller/SurveyController.java`

- [ ] **Step 1: Add builder methods to SurveyService interface**

```java
// Add to SurveyService.java:
    SurveyPage addPage(Long templateId, SurveyPage page, Long adminId);
    void deletePage(Long pageId);
    SurveySection addSection(Long pageId, SurveySection section, Long adminId);
    void deleteSection(Long sectionId);
    SurveyQuestion addQuestion(Long sectionId, SurveyQuestion question, Long adminId);
    void updateQuestion(Long questionId, SurveyQuestion question, Long adminId);
    void deleteQuestion(Long questionId);
    void reorderPages(Long templateId, List<ReorderItem> items);
    void reorderSections(Long pageId, List<ReorderItem> items);
    void reorderQuestions(Long sectionId, List<ReorderItem> items);
```

- [ ] **Step 2: Add visibility rule management**

```java
// Add to SurveyService.java:
    List<SurveyTemplateResponse.VisibilityRuleResponse> addVisibilityRule(
            Long templateId, String targetType, Long targetId,
            Long sourceQuestionId, String op, String value, Integer logicGroup, Long adminId);
    void deleteVisibilityRule(Long ruleId);
```

- [ ] **Step 3: Implement builder methods**

```java
// In SurveyServiceImpl.java, add:

@Override
@Transactional
public SurveyPage addPage(Long templateId, SurveyPage page, Long adminId) {
    int maxOrder = pageMapper.selectCount(new LambdaQueryWrapper<SurveyPage>()
            .eq(SurveyPage::getTemplateId, templateId));
    page.setTemplateId(templateId);
    page.setDisplayOrder(maxOrder + 1);
    page.setCreatedBy(adminId);
    page.setCreatedDate(System.currentTimeMillis());
    pageMapper.insert(page);
    return page;
}

// ... similar for addSection, addQuestion, deletePage, deleteSection, deleteQuestion

@Override
@Transactional
public void reorderPages(Long templateId, List<ReorderItem> items) {
    for (ReorderItem item : items) {
        SurveyPage page = pageMapper.selectById(item.getId());
        if (page != null && page.getTemplateId().equals(templateId)) {
            page.setDisplayOrder(item.getDisplayOrder());
            pageMapper.updateById(page);
        }
    }
}

// ReorderItem: reuse existing DTO from ticket config
```

- [ ] **Step 4: Add builder endpoints to SurveyController**

```java
// Add to SurveyController.java:
@PostMapping("/{templateId}/pages")
public ApiResult<SurveyTemplateResponse.PageResponse> addPage(...) { ... }

@DeleteMapping("/pages/{pageId}")
public ApiResult<Void> deletePage(...) { ... }

// ... etc for sections, questions, reorder
```

- [ ] **Step 5: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/service/SurveyService.java \
        ticket-system/backend/src/main/java/com/ticket/service/impl/SurveyServiceImpl.java \
        ticket-system/backend/src/main/java/com/ticket/controller/SurveyController.java
git commit -m "feat: survey builder — page/section/question CRUD + reorder endpoints"
```

### Task 2.5: Visibility Engine

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/service/SurveyVisibilityEngine.java`

- [ ] **Step 1: Create visibility engine**

```java
package com.ticket.service;

import com.ticket.dto.response.SurveyTemplateResponse;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SurveyVisibilityEngine {

    /**
     * Evaluate all visibility rules for a given set of answers.
     * Returns a Set of target IDs that should be HIDDEN.
     */
    public Set<String> evaluateHidden(SurveyTemplateResponse template, Map<Long, Object> answers) {
        Set<String> hidden = new HashSet<>();

        // Build dependency graph: sourceQuestionId → list of rules
        Map<Long, List<SurveyTemplateResponse.VisibilityRuleResponse>> depGraph = buildDepGraph(template);

        // Evaluate each page
        for (SurveyTemplateResponse.PageResponse page : template.getPages()) {
            if (!evaluateRules(page.getVisibilityRules(), answers)) {
                hidden.add("PAGE:" + page.getId());
            }
            // Sections
            for (SurveyTemplateResponse.SectionResponse section : page.getSections()) {
                if (!evaluateRules(section.getVisibilityRules(), answers)) {
                    hidden.add("SECTION:" + section.getId());
                }
                // Questions
                for (SurveyTemplateResponse.QuestionResponse q : section.getQuestions()) {
                    if (!evaluateRules(q.getVisibilityRules(), answers)) {
                        hidden.add("QUESTION:" + q.getId());
                    }
                }
            }
        }
        return hidden;
    }

    /**
     * Check if a group of rules is satisfied.
     * Same logicGroup → AND, different groups → OR.
     */
    private boolean evaluateRules(List<SurveyTemplateResponse.VisibilityRuleResponse> rules,
                                   Map<Long, Object> answers) {
        if (rules == null || rules.isEmpty()) return true;

        Map<Integer, List<SurveyTemplateResponse.VisibilityRuleResponse>> grouped = new HashMap<>();
        for (var r : rules) {
            grouped.computeIfAbsent(r.getLogicGroup(), k -> new ArrayList<>()).add(r);
        }

        // At least one group must be fully satisfied (OR across groups)
        for (List<SurveyTemplateResponse.VisibilityRuleResponse> group : grouped.values()) {
            boolean groupSatisfied = true;
            for (var r : group) {
                if (!evaluateSingleRule(r, answers)) {
                    groupSatisfied = false;
                    break;
                }
            }
            if (groupSatisfied) return true;
        }
        return false;
    }

    private boolean evaluateSingleRule(SurveyTemplateResponse.VisibilityRuleResponse rule,
                                        Map<Long, Object> answers) {
        Object answer = answers.get(rule.getSourceQuestionId());
        if (answer == null || "".equals(answer)) {
            return "not_answered".equals(rule.getOp());
        }
        if ("answered".equals(rule.getOp())) return true;

        String answerStr = answer.toString();
        String value = rule.getValue();

        switch (rule.getOp()) {
            case "eq": return answerStr.equals(value);
            case "neq": return !answerStr.equals(value);
            case "contains": return answerStr.contains(value);
            case "in": return Arrays.asList(value.split(",")).contains(answerStr);
            case "gt": return toDouble(answerStr) > toDouble(value);
            case "gte": return toDouble(answerStr) >= toDouble(value);
            case "lt": return toDouble(answerStr) < toDouble(value);
            case "lte": return toDouble(answerStr) <= toDouble(value);
            default: return true;
        }
    }

    private double toDouble(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }

    private Map<Long, List<SurveyTemplateResponse.VisibilityRuleResponse>> buildDepGraph(
            SurveyTemplateResponse template) {
        Map<Long, List<SurveyTemplateResponse.VisibilityRuleResponse>> graph = new HashMap<>();
        for (var page : template.getPages()) {
            for (var section : page.getSections()) {
                for (var q : section.getQuestions()) {
                    if (q.getVisibilityRules() != null) {
                        for (var rule : q.getVisibilityRules()) {
                            graph.computeIfAbsent(rule.getSourceQuestionId(), k -> new ArrayList<>()).add(rule);
                        }
                    }
                }
            }
        }
        return graph;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/service/SurveyVisibilityEngine.java
git commit -m "feat: SurveyVisibilityEngine — rule evaluation with AND/OR groups"
```

---

## Phase 3: Instance + Fill Flow

### Task 3.1: Instance DTOs + Service

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/request/CreateSurveyInstanceRequest.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/request/SaveAnswerRequest.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/request/SubmitSurveyRequest.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/response/SurveyInstanceResponse.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/response/SurveyFillResponse.java`
- Modify: `ticket-system/backend/src/main/java/com/ticket/service/SurveyService.java`
- Modify: `ticket-system/backend/src/main/java/com/ticket/service/impl/SurveyServiceImpl.java`

- [ ] **Step 1: Create request DTOs**

```java
// CreateSurveyInstanceRequest.java
package com.ticket.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSurveyInstanceRequest {
    @NotNull private Long templateId;
    @NotNull private Long assignedTo;
    private String triggerType = "MANUAL";
    private Long ticketId;
}

// SaveAnswerRequest.java
package com.ticket.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveAnswerRequest {
    @NotNull private Long questionId;
    private String value; // JSON string
}

// SubmitSurveyRequest.java
package com.ticket.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SubmitSurveyRequest {
    @NotNull private List<SaveAnswerRequest> answers;
}
```

- [ ] **Step 2: Create response DTOs**

```java
// SurveyFillResponse.java
package com.ticket.dto.response;

import lombok.Data;
import java.util.List;
import java.util.Set;
import java.util.Map;

@Data
public class SurveyFillResponse {
    private Long instanceId;
    private String instanceStatus;
    private String title;
    private List<SurveyTemplateResponse.PageResponse> pages;
    private Set<String> hiddenTargets;
    private Map<Long, String> existingAnswers;
}

// SurveyInstanceResponse.java (simplified)
package com.ticket.dto.response;

import lombok.Data;

@Data
public class SurveyInstanceResponse {
    private Long id;
    private String title;
    private String status;
    private Long assignedTo;
    private String triggerType;
    private Long ticketId;
    private String templateTitle;
    private Integer totalPages;
    private Integer completedPages;
    private Long createdDate;
}
```

- [ ] **Step 3: Add instance methods to SurveyService + implement**

```java
// SurveyService.java additions:
    SurveyInstanceResponse createInstance(CreateSurveyInstanceRequest request, Long adminId);
    List<SurveyInstanceResponse> listUserInstances(Long userId);
    List<SurveyInstanceResponse> listTemplateInstances(Long templateId);
    SurveyFillResponse getFillData(Long instanceId, Long userId);
    void saveAnswer(Long instanceId, SaveAnswerRequest request, Long userId);
    SurveyInstanceResponse submitSurvey(Long instanceId, SubmitSurveyRequest request, Long userId);
```

- [ ] **Step 4: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/dto/request/CreateSurveyInstanceRequest.java \
        ticket-system/backend/src/main/java/com/ticket/dto/request/SaveAnswerRequest.java \
        ticket-system/backend/src/main/java/com/ticket/dto/request/SubmitSurveyRequest.java \
        ticket-system/backend/src/main/java/com/ticket/dto/response/SurveyFillResponse.java \
        ticket-system/backend/src/main/java/com/ticket/dto/response/SurveyInstanceResponse.java \
        ticket-system/backend/src/main/java/com/ticket/service/SurveyService.java \
        ticket-system/backend/src/main/java/com/ticket/service/impl/SurveyServiceImpl.java
git commit -m "feat: survey instances — create, fill, submit with answer persistence"
```

### Task 3.2: SurveyFillController — User Fill Endpoints

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/controller/SurveyFillController.java`

- [ ] **Step 1: Create fill controller**

```java
package com.ticket.controller;

import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.SurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/surveys")
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Survey Fill", description = "User-facing survey fill endpoints")
public class SurveyFillController {

    private final SurveyService surveyService;

    public SurveyFillController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @GetMapping("/instances")
    @Operation(summary = "List my pending/completed survey instances")
    public ApiResult<List<SurveyInstanceResponse>> myInstances(@AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.listUserInstances(user.getUserId()));
    }

    @GetMapping("/instances/{id}/fill")
    @Operation(summary = "Get survey fill data (template + existing answers + hidden states)")
    public ApiResult<SurveyFillResponse> getFillData(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.getFillData(id, user.getUserId()));
    }

    @PutMapping("/instances/{id}/answers")
    @Operation(summary = "Save a single answer (auto-save per question/page switch)")
    public ApiResult<Void> saveAnswer(@PathVariable Long id,
                                       @Valid @RequestBody SaveAnswerRequest request,
                                       @AuthenticationPrincipal UserDetailsImpl user) {
        surveyService.saveAnswer(id, request, user.getUserId());
        return ApiResult.success();
    }

    @PostMapping("/instances/{id}/submit")
    @Operation(summary = "Submit completed survey instance")
    public ApiResult<SurveyInstanceResponse> submit(@PathVariable Long id,
                                                     @Valid @RequestBody SubmitSurveyRequest request,
                                                     @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResult.success(surveyService.submitSurvey(id, request, user.getUserId()));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add ticket-system/backend/src/main/java/com/ticket/controller/SurveyFillController.java
git commit -m "feat: SurveyFillController — user fill + submit endpoints"
```

---

## Phase 4: Frontend

### Task 4.1: API Layer + Store

**Files:**
- Create: `ticket-system/frontend/src/api/survey.js`
- Create: `ticket-system/frontend/src/stores/survey.js`

- [ ] **Step 1: Create API**

```javascript
import request from './request'

// Template CRUD
export function getTemplatesApi() { return request.get('/admin/surveys') }
export function getTemplateApi(id) { return request.get(`/admin/surveys/${id}`) }
export function createTemplateApi(data) { return request.post('/admin/surveys', data) }
export function updateTemplateApi(id, data) { return request.put(`/admin/surveys/${id}`, data) }
export function deleteTemplateApi(id) { return request.delete(`/admin/surveys/${id}`) }

// Builder
export function addPageApi(templateId, data) { return request.post(`/admin/surveys/${templateId}/pages`, data) }
export function deletePageApi(pageId) { return request.delete(`/admin/surveys/pages/${pageId}`) }
export function addSectionApi(pageId, data) { return request.post(`/admin/surveys/pages/${pageId}/sections`, data) }
export function deleteSectionApi(sectionId) { return request.delete(`/admin/surveys/sections/${sectionId}`) }
export function addQuestionApi(sectionId, data) { return request.post(`/admin/surveys/sections/${sectionId}/questions`, data) }
export function updateQuestionApi(questionId, data) { return request.put(`/admin/surveys/questions/${questionId}`, data) }
export function deleteQuestionApi(questionId) { return request.delete(`/admin/surveys/questions/${questionId}`) }
export function reorderPagesApi(templateId, items) { return request.put(`/admin/surveys/${templateId}/pages/reorder`, { items }) }
export function addRuleApi(templateId, data) { return request.post(`/admin/surveys/${templateId}/rules`, data) }
export function deleteRuleApi(ruleId) { return request.delete(`/admin/surveys/rules/${ruleId}`) }

// Instances
export function createInstanceApi(data) { return request.post('/admin/surveys/instances', data) }
export function getTemplateInstancesApi(templateId) { return request.get(`/admin/surveys/instances?templateId=${templateId}`) }

// Fill
export function getMyInstancesApi() { return request.get('/surveys/instances') }
export function getFillDataApi(instanceId) { return request.get(`/surveys/instances/${instanceId}/fill`) }
export function saveAnswerApi(instanceId, data) { return request.put(`/surveys/instances/${instanceId}/answers`, data) }
export function submitSurveyApi(instanceId, data) { return request.post(`/surveys/instances/${instanceId}/submit`, data) }
```

- [ ] **Step 2: Create Pinia store**

```javascript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTemplatesApi, getTemplateApi, createTemplateApi, deleteTemplateApi } from '@/api/survey'

export const useSurveyStore = defineStore('survey', () => {
  const templates = ref([])
  const currentTemplate = ref(null)
  const loading = ref(false)

  async function fetchTemplates() {
    loading.value = true
    try {
      const { data } = await getTemplatesApi()
      if (data?.code === 200) templates.value = data.data || []
    } finally { loading.value = false }
  }

  async function fetchTemplate(id) {
    const { data } = await getTemplateApi(id)
    if (data?.code === 200) currentTemplate.value = data.data
    return data
  }

  async function createTemplate(form) {
    const { data } = await createTemplateApi(form)
    if (data.code === 200) await fetchTemplates()
    return data
  }

  async function deleteTemplate(id) {
    const { data } = await deleteTemplateApi(id)
    if (data.code === 200) await fetchTemplates()
    return data
  }

  return { templates, currentTemplate, loading, fetchTemplates, fetchTemplate, createTemplate, deleteTemplate }
})
```

- [ ] **Step 3: Commit**

```bash
git add ticket-system/frontend/src/api/survey.js \
        ticket-system/frontend/src/stores/survey.js
git commit -m "feat: survey frontend — API layer + Pinia store"
```

### Task 4.2: SurveyListView.vue — Template Management

**Files:**
- Create: `ticket-system/frontend/src/views/admin/SurveyListView.vue`
- Modify: `ticket-system/frontend/src/router/index.js`
- Modify: `ticket-system/frontend/src/layouts/AppLayout.vue`

- [ ] **Step 1: Create list view (template card grid)**

Standard admin list page pattern (consistent with existing UserListView). Each template shown as a card with: title, status badge (DRAFT/PUBLISHED/ARCHIVED), page count, question count, action buttons.

- [ ] **Step 2: Add route + nav**

```javascript
// Router: add to admin children
{ path: 'admin/surveys', name: 'AdminSurveys', component: () => import('@/views/admin/SurveyListView.vue'), meta: { requiresAuth: true, requiresAdmin: true } }

// Layout: add nav link
<router-link v-if="authStore.isAdmin" to="/admin/surveys" class="app-nav-link">Surveys</router-link>
```

- [ ] **Step 3: Commit**

```bash
git add ticket-system/frontend/src/views/admin/SurveyListView.vue \
        ticket-system/frontend/src/router/index.js \
        ticket-system/frontend/src/layouts/AppLayout.vue
git commit -m "feat: SurveyListView — admin template management with card grid"
```

### Task 4.3: SurveyBuilderView.vue — 3-Column Editor

Interactive three-column builder: left structure tree (drag to reorder), center canvas (live preview), right properties panel. Each node (page/section/question) can be added, edited, reordered, or deleted. Visibility rules configured via RuleEditorModal.vue.

### Task 4.4: SurveyFillView.vue — Multi-Page Fill

Split layout: left navigation (page list with status icons), right content (current page's sections and questions). Auto-saves on page switch. Visibility engine recalculates on answer changes. Submit with full validation.

### Task 4.5: SurveyResultsView.vue + Dashboard

Results page aggregates answers by question type (choice → chart, text → list). Dashboard shows pending survey cards.

---

## Phase 5: Versioning, Import/Export, Notifications

### Task 5.1: Template Versioning + Clone + Import/Export

Add endpoints for clone (POST with source template ID), new version, export (GET returns JSON), import (POST parses JSON and creates DRAFT). Implement in SurveyServiceImpl.

### Task 5.2: Notifications Integration

Reuse existing KafkaEventPublisher to send `survey.instance.created` events. NotificationServiceImpl handles push to assigned user. Add Kafka consumer for survey events.

---

**Estimated total:** 25-30h (5 phases, ~25 tasks)
