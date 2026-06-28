-- V18: Change audit_log.detail from JSON to TEXT
-- detail is a human-readable free-text field, not structured JSON data
ALTER TABLE audit_log MODIFY COLUMN detail TEXT DEFAULT NULL COMMENT 'Free-text detail';
