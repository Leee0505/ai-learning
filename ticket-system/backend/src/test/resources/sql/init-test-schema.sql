-- H2-compatible test schema (MySQL mode)
-- Adapted from V1__init_schema.sql and V3__create_ticket_tables.sql

CREATE TABLE IF NOT EXISTS `tenant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `slug` VARCHAR(50) NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `created_date` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_slug` (`slug`)
);

INSERT INTO tenant (id, name, slug, status, created_date) VALUES (1, 'Default', 'default', 1, 0);

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `username` VARCHAR(50) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `password` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    `status` SMALLINT NOT NULL DEFAULT 1,
    `created_by` BIGINT NOT NULL DEFAULT 0,
    `created_date` BIGINT NOT NULL,
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_username` (`tenant_id`, `username`),
    UNIQUE KEY `uk_tenant_email` (`tenant_id`, `email`)
);

CREATE TABLE IF NOT EXISTS `invite_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `token` VARCHAR(64) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `expires_at` BIGINT NOT NULL,
    `used` SMALLINT NOT NULL DEFAULT 0,
    `created_by` BIGINT NOT NULL,
    `created_date` BIGINT NOT NULL,
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_token` (`tenant_id`, `token`)
);

CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `user_id` BIGINT NOT NULL,
    `action` VARCHAR(50) NOT NULL,
    `target_type` VARCHAR(50) DEFAULT NULL,
    `target_id` BIGINT DEFAULT NULL,
    `detail` TEXT DEFAULT NULL,
    `ip_address` VARCHAR(45) DEFAULT NULL,
    `created_by` BIGINT NOT NULL,
    `created_date` BIGINT NOT NULL,
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `ticket` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `title` VARCHAR(255) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    `priority` VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    `category` VARCHAR(50) DEFAULT NULL,
    `assigned_to` BIGINT DEFAULT NULL,
    `resolved_date` BIGINT DEFAULT NULL,
    `closed_date` BIGINT DEFAULT NULL,
    `created_by` BIGINT NOT NULL,
    `created_date` BIGINT NOT NULL,
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `ticket_reply` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `ticket_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `is_internal` SMALLINT NOT NULL DEFAULT 0,
    `created_by` BIGINT NOT NULL,
    `created_date` BIGINT NOT NULL,
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`ticket_id`) REFERENCES `ticket` (`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `ticket_field_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `name` VARCHAR(100) NOT NULL,
    `field_key` VARCHAR(50) NOT NULL,
    `field_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    `options` VARCHAR(1000) DEFAULT NULL,
    `required` TINYINT NOT NULL DEFAULT 0,
    `active` TINYINT NOT NULL DEFAULT 1,
    `display_order` INT NOT NULL DEFAULT 0,
    `created_by` BIGINT NOT NULL,
    `created_date` BIGINT NOT NULL,
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_field_key` (`tenant_id`, `field_key`)
);

CREATE TABLE IF NOT EXISTS `sla_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `priority` VARCHAR(20) NOT NULL,
    `response_minutes` INT NOT NULL,
    `resolution_minutes` INT NOT NULL,
    `active` TINYINT NOT NULL DEFAULT 1,
    `created_by` BIGINT NOT NULL,
    `created_date` BIGINT NOT NULL,
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_priority` (`tenant_id`, `priority`)
);

-- Seed default SLA rules for tests
INSERT INTO sla_config (priority, response_minutes, resolution_minutes, active, created_by, created_date) VALUES
('URGENT', 60, 240, 1, 0, 0),
('HIGH', 240, 1440, 1, 0, 0),
('MEDIUM', 480, 2880, 1, 0, 0),
('LOW', 1440, 5760, 1, 0, 0);

-- Seed default custom fields for tests
INSERT INTO ticket_field_config (name, field_key, field_type, options, display_order, active, required, created_by, created_date) VALUES
('Environment', 'environment', 'SINGLE_SELECT', '{"items":["Production","Staging","Development"]}', 1, 1, 0, 0, 0),
('Version', 'version', 'TEXT', NULL, 2, 1, 0, 0, 0);

CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(30) NOT NULL,
    `ticket_id` BIGINT DEFAULT NULL,
    `title` VARCHAR(255) NOT NULL,
    `message` TEXT DEFAULT NULL,
    `is_read` TINYINT NOT NULL DEFAULT 0,
    `created_date` BIGINT NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `ticket_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `ticket_id` BIGINT DEFAULT NULL,
    `reply_id` BIGINT DEFAULT NULL,
    `filename` VARCHAR(255) NOT NULL,
    `original_filename` VARCHAR(255) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `content_type` VARCHAR(100) DEFAULT NULL,
    `storage_path` VARCHAR(500) NOT NULL,
    `created_by` BIGINT NOT NULL,
    `created_date` BIGINT NOT NULL,
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`)
);
