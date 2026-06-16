-- ============================================================
-- V8: Knowledge base articles for FAQ / documentation
-- ============================================================
CREATE TABLE IF NOT EXISTS knowledge_article (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    title             VARCHAR(300) NOT NULL,
    content           MEDIUMTEXT NOT NULL COMMENT 'Markdown content',
    category          VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    tags              VARCHAR(500) DEFAULT NULL COMMENT 'Comma-separated tags',
    view_count        BIGINT NOT NULL DEFAULT 0,
    created_by        BIGINT NOT NULL,
    created_date      BIGINT NOT NULL,
    last_modified_by  BIGINT DEFAULT NULL,
    last_modified_date BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_kb_category (category),
    FULLTEXT idx_kb_title_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed: basic FAQ articles
INSERT INTO knowledge_article (title, content, category, tags, created_by, created_date) VALUES
('How to change your password',
 '## Changing Your Password\n\n1. Click **Profile** in the top navigation bar.\n2. Enter your **current password**.\n3. Enter your **new password** (minimum 6 characters).\n4. Confirm the new password and click **Change Password**.\n\n> Your session will remain active after the change.',
 'GENERAL', 'password, account, security', 0, UNIX_TIMESTAMP() * 1000),

('How to submit a ticket',
 '## Submitting a Ticket\n\n1. Click **New Ticket** on the Tickets page.\n2. Fill in the **title** — be specific (e.g., "Login page shows 500 error").\n3. Select the **category** and **priority**.\n4. Write a detailed **description** using the rich text editor.\n   - Include steps to reproduce\n   - Attach screenshots (drag & drop or use the image button)\n5. Click **Create Ticket**.\n\nYour ticket will appear in the list immediately.',
 'GENERAL', 'ticket, how-to, getting-started', 0, UNIX_TIMESTAMP() * 1000),

('Understanding ticket priorities',
 '## Ticket Priority Levels\n\n| Priority | Response SLA | Description |\n|----------|-------------|-------------|\n| **Urgent** | 1 hour | Critical system outage, security breach, data loss |\n| **High** | 4 hours | Major feature broken, blocking work |\n| **Medium** | 24 hours | Partial feature issue, workaround available |\n| **Low** | 48 hours | Cosmetic issue, feature request |\n\nSelect the priority that best matches the impact on your work.',
 'GENERAL', 'priority, sla, policy', 0, UNIX_TIMESTAMP() * 1000),

('Browser compatibility',
 '## Supported Browsers\n\n- **Google Chrome** 90+ (recommended)\n- **Mozilla Firefox** 90+\n- **Microsoft Edge** 90+\n- **Safari** 14+\n\n## Known Issues\n\n- Internet Explorer is **not supported**.\n- Safari may have rendering differences with Markdown tables.\n\nIf you experience browser-specific issues, please include your browser version in the ticket.',
 'BUG', 'browser, compatibility, troubleshooting', 0, UNIX_TIMESTAMP() * 1000);
