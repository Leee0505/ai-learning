-- V12: Survey System — templates, pages, sections, questions, visibility rules, instances, answers
-- Multi-tenant: tenant_id only on top-level entities (survey_template, survey_instance)

-- 1. Survey template (top-level container, tenant-scoped)
CREATE TABLE IF NOT EXISTS survey_template (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID for data isolation',
    title             VARCHAR(200) NOT NULL COMMENT 'Template title',
    description       TEXT COMMENT 'Template description/notes',
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT | PUBLISHED | ARCHIVED',
    version           INT NOT NULL DEFAULT 1 COMMENT 'Version number — incremented on publish',
    origin_id         BIGINT DEFAULT NULL COMMENT 'Points to original template when this is a version copy',
    allow_resubmit    TINYINT NOT NULL DEFAULT 0 COMMENT '1=allow multiple submissions from same user',
    created_by        BIGINT NOT NULL,
    created_date      BIGINT NOT NULL,
    last_modified_by  BIGINT DEFAULT NULL,
    last_modified_date BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_st_tenant (tenant_id),
    INDEX idx_st_origin (origin_id),
    INDEX idx_st_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Survey page (belongs to template, for multi-page surveys)
CREATE TABLE IF NOT EXISTS survey_page (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    template_id       BIGINT NOT NULL COMMENT 'FK → survey_template.id',
    title             VARCHAR(200) NOT NULL COMMENT 'Page title',
    display_order     INT NOT NULL DEFAULT 0 COMMENT 'Display ordering within template',
    created_by        BIGINT NOT NULL,
    created_date      BIGINT NOT NULL,
    last_modified_by  BIGINT DEFAULT NULL,
    last_modified_date BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_sp_template (template_id),
    CONSTRAINT fk_sp_template FOREIGN KEY (template_id) REFERENCES survey_template(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Survey section (belongs to page, groups related questions)
CREATE TABLE IF NOT EXISTS survey_section (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    page_id           BIGINT NOT NULL COMMENT 'FK → survey_page.id',
    title             VARCHAR(200) NOT NULL COMMENT 'Section title',
    description       VARCHAR(500) COMMENT 'Optional section description',
    display_order     INT NOT NULL DEFAULT 0 COMMENT 'Display ordering within page',
    created_by        BIGINT NOT NULL,
    created_date      BIGINT NOT NULL,
    last_modified_by  BIGINT DEFAULT NULL,
    last_modified_date BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_ss_page (page_id),
    CONSTRAINT fk_ss_page FOREIGN KEY (page_id) REFERENCES survey_page(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Survey question (belongs to section, multiple types)
CREATE TABLE IF NOT EXISTS survey_question (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    section_id        BIGINT NOT NULL COMMENT 'FK → survey_section.id',
    type              VARCHAR(30) NOT NULL COMMENT 'SINGLE_CHOICE | MULTI_CHOICE | TEXT | TEXTAREA | DATE | DROPDOWN | CASCADER | RATING | TABLE',
    title             VARCHAR(500) NOT NULL COMMENT 'Question title/prompt',
    description       VARCHAR(1000) COMMENT 'Optional helper text',
    options           JSON COMMENT 'JSON for choice options, cascader tree, table columns, etc.',
    required          INT NOT NULL DEFAULT 0 COMMENT '0=optional, 1=required',
    display_order     INT NOT NULL DEFAULT 0 COMMENT 'Display ordering within section',
    created_by        BIGINT NOT NULL,
    created_date      BIGINT NOT NULL,
    last_modified_by  BIGINT DEFAULT NULL,
    last_modified_date BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_sq_section (section_id),
    INDEX idx_sq_type (type),
    CONSTRAINT fk_sq_section FOREIGN KEY (section_id) REFERENCES survey_section(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Visibility rule (conditional show/hide of pages/sections/questions)
CREATE TABLE IF NOT EXISTS survey_visibility_rule (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    template_id       BIGINT NOT NULL COMMENT 'FK → survey_template.id',
    target_type       VARCHAR(20) NOT NULL COMMENT 'PAGE | SECTION | QUESTION',
    target_id         BIGINT NOT NULL COMMENT 'FK to target table based on target_type',
    source_question_id BIGINT NOT NULL COMMENT 'FK → survey_question.id — the question whose answer triggers this rule',
    op                VARCHAR(20) NOT NULL COMMENT 'EQ | NEQ | IN | NOT_IN | GT | LT | GTE | LTE | CONTAINS',
    value             VARCHAR(500) NOT NULL COMMENT 'Comparison value (or JSON array for IN/NOT_IN)',
    logic_group       VARCHAR(50) DEFAULT NULL COMMENT 'Group identifier for AND/OR logic grouping',
    display_order     INT NOT NULL DEFAULT 0 COMMENT 'Evaluation order within same logic_group',
    created_by        BIGINT NOT NULL,
    created_date      BIGINT NOT NULL,
    last_modified_by  BIGINT DEFAULT NULL,
    last_modified_date BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_svr_template (template_id),
    INDEX idx_svr_source (source_question_id),
    INDEX idx_svr_target (target_type, target_id),
    CONSTRAINT fk_svr_template FOREIGN KEY (template_id) REFERENCES survey_template(id) ON DELETE CASCADE,
    CONSTRAINT fk_svr_source_q FOREIGN KEY (source_question_id) REFERENCES survey_question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Survey instance (a specific survey assignment/session, tenant-scoped)
CREATE TABLE IF NOT EXISTS survey_instance (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID for data isolation',
    template_id       BIGINT NOT NULL COMMENT 'FK → survey_template.id',
    title             VARCHAR(200) NOT NULL COMMENT 'Instance title (defaults to template title)',
    status            VARCHAR(20) NOT NULL DEFAULT 'READY_TO_START' COMMENT 'READY_TO_START | IN_PROGRESS | SUBMITTED | COMPLETED',
    assigned_to       BIGINT NOT NULL COMMENT 'User ID the survey is assigned to',
    trigger_type      VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'TICKET | MANUAL',
    ticket_id         BIGINT DEFAULT NULL COMMENT 'FK → ticket.id when trigger_type=TICKET',
    created_by        BIGINT NOT NULL,
    created_date      BIGINT NOT NULL,
    last_modified_by  BIGINT DEFAULT NULL,
    last_modified_date BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_si_tenant (tenant_id),
    INDEX idx_si_template (template_id),
    INDEX idx_si_assigned (assigned_to),
    INDEX idx_si_status (status),
    INDEX idx_si_ticket (ticket_id),
    CONSTRAINT fk_si_template FOREIGN KEY (template_id) REFERENCES survey_template(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Survey instance page (tracks per-page completion within an instance)
CREATE TABLE IF NOT EXISTS survey_instance_page (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    instance_id BIGINT NOT NULL COMMENT 'FK → survey_instance.id',
    page_id     BIGINT NOT NULL COMMENT 'FK → survey_page.id',
    status      VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'NOT_STARTED | IN_PROGRESS | COMPLETED',
    PRIMARY KEY (id),
    UNIQUE KEY uk_instance_page (instance_id, page_id),
    INDEX idx_sip_instance (instance_id),
    CONSTRAINT fk_sip_instance FOREIGN KEY (instance_id) REFERENCES survey_instance(id) ON DELETE CASCADE,
    CONSTRAINT fk_sip_page FOREIGN KEY (page_id) REFERENCES survey_page(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Survey answer (individual question answers within an instance)
CREATE TABLE IF NOT EXISTS survey_answer (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    instance_id BIGINT NOT NULL COMMENT 'FK → survey_instance.id',
    question_id BIGINT NOT NULL COMMENT 'FK → survey_question.id',
    value       JSON COMMENT 'Answer value — type depends on question type',
    PRIMARY KEY (id),
    UNIQUE KEY uk_answer_instance_q (instance_id, question_id),
    INDEX idx_sa_instance (instance_id),
    CONSTRAINT fk_sa_instance FOREIGN KEY (instance_id) REFERENCES survey_instance(id) ON DELETE CASCADE,
    CONSTRAINT fk_sa_question FOREIGN KEY (question_id) REFERENCES survey_question(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
