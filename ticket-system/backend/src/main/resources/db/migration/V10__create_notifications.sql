-- V10: Real-time notification persistence for WebSocket push
CREATE TABLE IF NOT EXISTS notification (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    user_id       BIGINT NOT NULL COMMENT 'Target user',
    type          VARCHAR(30) NOT NULL COMMENT 'TICKET_CREATED | TICKET_ASSIGNED | TICKET_REPLIED | TICKET_RESOLVED | TICKET_OVERDUE',
    ticket_id     BIGINT DEFAULT NULL COMMENT 'Related ticket',
    title         VARCHAR(255) NOT NULL COMMENT 'Short summary',
    message       TEXT DEFAULT NULL COMMENT 'Optional detail',
    is_read       TINYINT NOT NULL DEFAULT 0,
    created_date  BIGINT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_notif_user_read (user_id, is_read),
    INDEX idx_notif_date (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
