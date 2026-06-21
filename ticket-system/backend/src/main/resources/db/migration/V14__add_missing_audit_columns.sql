-- V14: Add missing audit columns to survey_instance_page and survey_answer
ALTER TABLE survey_instance_page
    ADD COLUMN created_by        BIGINT NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN created_date      BIGINT NOT NULL DEFAULT 0 AFTER created_by,
    ADD COLUMN last_modified_by  BIGINT DEFAULT NULL AFTER created_date,
    ADD COLUMN last_modified_date BIGINT DEFAULT NULL AFTER last_modified_by;

ALTER TABLE survey_answer
    ADD COLUMN created_by        BIGINT NOT NULL DEFAULT 0 AFTER value,
    ADD COLUMN created_date      BIGINT NOT NULL DEFAULT 0 AFTER created_by,
    ADD COLUMN last_modified_by  BIGINT DEFAULT NULL AFTER created_date,
    ADD COLUMN last_modified_date BIGINT DEFAULT NULL AFTER last_modified_by;
