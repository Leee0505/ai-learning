-- H2-compatible schema for integration tests
-- MySQL-specific clauses (ENGINE, CHARSET) removed for H2 compatibility

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `password` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    `status` TINYINT NOT NULL DEFAULT 1,
    `created_by` BIGINT NOT NULL DEFAULT 0,
    `created_date` BIGINT NOT NULL,
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`username`),
    UNIQUE (`email`)
);

CREATE TABLE IF NOT EXISTS `invite_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `token` VARCHAR(64) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `expires_at` BIGINT NOT NULL,
    `used` TINYINT NOT NULL DEFAULT 0,
    `created_by` BIGINT NOT NULL,
    `created_date` BIGINT NOT NULL,
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`token`)
);
