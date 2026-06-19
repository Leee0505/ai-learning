-- V11: Multi-tenancy — tenant table + tenant_id columns + composite unique keys
-- Strategy: shared database with tenant_id column isolation
-- Existing data gets tenant_id = 1 (Default tenant)

-- 1. Create tenant table
CREATE TABLE IF NOT EXISTS tenant (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL COMMENT 'Display name of the tenant/organization',
    slug          VARCHAR(50) NOT NULL COMMENT 'URL-friendly unique identifier',
    status        TINYINT NOT NULL DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    created_date  BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Seed default tenant (id=1 for all existing data)
INSERT INTO tenant (id, name, slug, status, created_date) VALUES
(1, 'Default', 'default', 1, UNIX_TIMESTAMP() * 1000);

-- 3. Add tenant_id to all core tables
ALTER TABLE user ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID for data isolation';
ALTER TABLE invite_token ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID for data isolation';
ALTER TABLE audit_log ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID for data isolation';
ALTER TABLE ticket ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID for data isolation';
ALTER TABLE ticket_reply ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID for data isolation';
ALTER TABLE ticket_attachment ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID for data isolation';
ALTER TABLE reply_template ADD COLUMN tenant_id BIGINT DEFAULT NULL COMMENT 'Tenant ID — NULL = system default template';
ALTER TABLE knowledge_article ADD COLUMN tenant_id BIGINT DEFAULT NULL COMMENT 'Tenant ID — NULL = system default article';
ALTER TABLE ticket_field_config ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID for data isolation';
ALTER TABLE sla_config ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID for data isolation';
ALTER TABLE notification ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'Tenant ID for data isolation';

-- 4. Drop old single-column unique keys
ALTER TABLE user DROP INDEX uk_username;
ALTER TABLE user DROP INDEX uk_email;
ALTER TABLE invite_token DROP INDEX uk_token;
ALTER TABLE ticket_field_config DROP INDEX uk_field_key;
ALTER TABLE sla_config DROP INDEX uk_priority;

-- 5. Rebuild unique keys as composite with tenant_id
ALTER TABLE user ADD UNIQUE KEY uk_tenant_username (tenant_id, username);
ALTER TABLE user ADD UNIQUE KEY uk_tenant_email (tenant_id, email);
ALTER TABLE invite_token ADD UNIQUE KEY uk_tenant_token (tenant_id, token);
ALTER TABLE ticket_field_config ADD UNIQUE KEY uk_tenant_field_key (tenant_id, field_key);
ALTER TABLE sla_config ADD UNIQUE KEY uk_tenant_priority (tenant_id, priority);

-- 6. Add tenant indexes for query isolation performance
CREATE INDEX idx_user_tenant ON user (tenant_id);
CREATE INDEX idx_ticket_tenant ON ticket (tenant_id);
CREATE INDEX idx_ticket_reply_tenant ON ticket_reply (tenant_id);
CREATE INDEX idx_audit_log_tenant ON audit_log (tenant_id);
CREATE INDEX idx_notification_tenant ON notification (tenant_id);
