-- ============================================================
-- V7: Reply templates for quick agent responses
-- ============================================================
CREATE TABLE IF NOT EXISTS reply_template (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    title             VARCHAR(200) NOT NULL,
    content           TEXT NOT NULL,
    category          VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    created_by        BIGINT NOT NULL,
    created_date      BIGINT NOT NULL,
    last_modified_by  BIGINT DEFAULT NULL,
    last_modified_date BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_template_category (category),
    KEY idx_template_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed: common support templates
INSERT INTO reply_template (title, content, category, created_by, created_date) VALUES
('Request screenshot', 'Could you please provide a screenshot of the issue? This will help us diagnose the problem more quickly.\n\nYou can attach images directly — drag & drop or click the image icon in the toolbar.', 'GENERAL', 0, UNIX_TIMESTAMP() * 1000),
('Investigating', 'Thank you for reporting this. We are currently investigating the issue and will update you as soon as we have more information.\n\n**Reference:** #{{ticketId}}', 'GENERAL', 0, UNIX_TIMESTAMP() * 1000),
('Resolution confirmed', 'This issue should now be resolved. Please verify on your end and let us know if you experience any further problems.\n\nIf everything looks good, we will close this ticket.', 'GENERAL', 0, UNIX_TIMESTAMP() * 1000),
('Need more info', 'We need some additional information to proceed:\n\n1. What steps did you take before the issue occurred?\n2. What browser/OS are you using?\n3. Does this happen consistently or intermittently?', 'BUG', 0, UNIX_TIMESTAMP() * 1000),
('Escalate to dev team', 'This issue has been escalated to our development team for further investigation. The dev team will review the details and provide a fix timeline.\n\nThank you for your patience.', 'BUG', 0, UNIX_TIMESTAMP() * 1000);
