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
    response_minutes    INT NOT NULL COMMENT 'First response SLA in minutes',
    resolution_minutes  INT NOT NULL COMMENT 'Resolution SLA in minutes',
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
INSERT INTO sla_config (priority, response_minutes, resolution_minutes, created_by, created_date) VALUES
('URGENT', 60, 240, 0, UNIX_TIMESTAMP() * 1000),
('HIGH', 240, 1440, 0, UNIX_TIMESTAMP() * 1000),
('MEDIUM', 480, 2880, 0, UNIX_TIMESTAMP() * 1000),
('LOW', 1440, 5760, 0, UNIX_TIMESTAMP() * 1000);

-- Seed default custom fields
INSERT INTO ticket_field_config (name, field_key, field_type, options, display_order, created_by, created_date) VALUES
('Environment', 'environment', 'SINGLE_SELECT', '{"items":["Production","Staging","Development"]}', 1, 0, UNIX_TIMESTAMP() * 1000),
('Version', 'version', 'TEXT', NULL, 2, 0, UNIX_TIMESTAMP() * 1000);
