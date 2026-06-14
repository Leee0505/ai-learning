-- V3: Create ticket core tables

CREATE TABLE IF NOT EXISTS `ticket` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(255) NOT NULL COMMENT 'Ticket title',
    `description` TEXT DEFAULT NULL COMMENT 'Detailed description',
    `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, IN_PROGRESS, RESOLVED, CLOSED',
    `priority` VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW, MEDIUM, HIGH, URGENT',
    `category` VARCHAR(50) DEFAULT NULL COMMENT 'BUG, FEATURE_REQUEST, GENERAL_QUESTION, ACCOUNT_ISSUE, OTHER',
    `assigned_to` BIGINT DEFAULT NULL COMMENT 'Assigned agent user ID',
    `resolved_date` BIGINT DEFAULT NULL COMMENT 'Unix timestamp (ms) when resolved',
    `closed_date` BIGINT DEFAULT NULL COMMENT 'Unix timestamp (ms) when closed',
    `created_by` BIGINT NOT NULL COMMENT 'Ticket creator user ID',
    `created_date` BIGINT NOT NULL COMMENT 'Unix timestamp (ms)',
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_ticket_status` (`status`),
    KEY `idx_ticket_priority` (`priority`),
    KEY `idx_ticket_category` (`category`),
    KEY `idx_ticket_assigned_to` (`assigned_to`),
    KEY `idx_ticket_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ticket_reply` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `ticket_id` BIGINT NOT NULL COMMENT 'FK to ticket table',
    `user_id` BIGINT NOT NULL COMMENT 'Reply author user ID',
    `content` TEXT NOT NULL COMMENT 'Reply content',
    `is_internal` TINYINT NOT NULL DEFAULT 0 COMMENT '0=public reply, 1=internal note (agent only)',
    `created_by` BIGINT NOT NULL COMMENT 'Reply author user ID (audit)',
    `created_date` BIGINT NOT NULL COMMENT 'Unix timestamp (ms)',
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_reply_ticket_id` (`ticket_id`),
    KEY `idx_reply_user_id` (`user_id`),
    CONSTRAINT `fk_reply_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `ticket` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ticket_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `ticket_id` BIGINT DEFAULT NULL COMMENT 'FK to ticket table (nullable for reply attachments)',
    `reply_id` BIGINT DEFAULT NULL COMMENT 'FK to ticket_reply table (nullable for ticket-level attachments)',
    `filename` VARCHAR(255) NOT NULL COMMENT 'Storage filename (UUID + extension)',
    `original_filename` VARCHAR(255) NOT NULL COMMENT 'Original uploaded filename',
    `file_size` BIGINT NOT NULL COMMENT 'File size in bytes',
    `content_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME type',
    `storage_path` VARCHAR(500) NOT NULL COMMENT 'Absolute or relative path on disk',
    `created_by` BIGINT NOT NULL COMMENT 'Uploader user ID',
    `created_date` BIGINT NOT NULL COMMENT 'Unix timestamp (ms)',
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_attach_ticket_id` (`ticket_id`),
    KEY `idx_attach_reply_id` (`reply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
