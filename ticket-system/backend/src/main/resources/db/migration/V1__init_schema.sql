-- V1: Initialize core tables for authentication

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `password` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '0 for self-registration',
    `created_date` BIGINT NOT NULL COMMENT 'Unix timestamp in milliseconds',
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_user_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `invite_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `token` VARCHAR(64) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `expires_at` BIGINT NOT NULL COMMENT 'Unix timestamp in milliseconds',
    `used` TINYINT NOT NULL DEFAULT 0 COMMENT '0=unused, 1=used',
    `created_by` BIGINT NOT NULL COMMENT 'Admin who created the invitation',
    `created_date` BIGINT NOT NULL COMMENT 'Unix timestamp in milliseconds',
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token`),
    KEY `idx_invite_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT 'Actor user ID',
    `action` VARCHAR(50) NOT NULL COMMENT 'LOGIN, LOGOUT, REGISTER, INVITE_AGENT, ACCEPT_INVITE',
    `target_type` VARCHAR(50) DEFAULT NULL COMMENT 'USER, INVITE_TOKEN',
    `target_id` BIGINT DEFAULT NULL,
    `detail` JSON DEFAULT NULL COMMENT 'Flexible payload',
    `ip_address` VARCHAR(45) DEFAULT NULL,
    `created_by` BIGINT NOT NULL COMMENT 'Same as user_id for unified audit queries',
    `created_date` BIGINT NOT NULL COMMENT 'Unix timestamp in milliseconds',
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_audit_user` (`user_id`),
    KEY `idx_audit_action` (`action`),
    KEY `idx_audit_created_date` (`created_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
