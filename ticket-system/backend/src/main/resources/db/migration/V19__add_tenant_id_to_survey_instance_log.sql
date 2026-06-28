-- V19: Add tenant_id column for TenantLineInnerInterceptor
ALTER TABLE survey_instance_log ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
