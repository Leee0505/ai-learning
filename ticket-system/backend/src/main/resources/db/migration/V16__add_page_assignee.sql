-- V16: Add assignee support at page level for survey instances

ALTER TABLE survey_instance_page
    ADD COLUMN assigned_to BIGINT COMMENT 'FK → user.id — page-level assignee';

CREATE INDEX idx_sip_assigned_to ON survey_instance_page (assigned_to);
