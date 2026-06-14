-- H2-compatible test schema (MySQL mode)
-- Adapted from V1__init_schema.sql and V3__create_ticket_tables.sql

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
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
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
);

CREATE TABLE IF NOT EXISTS `invite_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `token` VARCHAR(64) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `expires_at` BIGINT NOT NULL,
    `used` SMALLINT NOT NULL DEFAULT 0,
    `created_by` BIGINT NOT NULL,
    `created_date` BIGINT NOT NULL,
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token`)
);

CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
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

CREATE TABLE IF NOT EXISTS `ticket_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
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
