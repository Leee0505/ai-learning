-- V17: Granular activity log for survey instances
-- Tracks page completions, reopens, answer changes, submissions
CREATE TABLE IF NOT EXISTS survey_instance_log (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    instance_id     BIGINT NOT NULL,
    page_id         BIGINT DEFAULT NULL,
    question_id     BIGINT DEFAULT NULL,
    action          VARCHAR(30) NOT NULL COMMENT 'COMPLETE_PAGE, REOPEN_PAGE, REOPEN_INSTANCE, SUBMIT, ANSWER_SAVED, INSTANCE_CREATED',
    user_id         BIGINT NOT NULL,
    detail          VARCHAR(500) DEFAULT NULL,
    created_date    BIGINT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_sil_instance (instance_id),
    INDEX idx_sil_instance_date (instance_id, created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
