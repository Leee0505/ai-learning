-- V15: Change survey_answer.value from JSON to TEXT
-- JSON type rejects plain strings; answer values are mostly plain text

ALTER TABLE survey_answer
    MODIFY COLUMN value TEXT COMMENT 'Answer value';
