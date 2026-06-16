-- ============================================================
-- V6: Add is_edited flag for reply edit tracking
-- ============================================================
ALTER TABLE ticket_reply
    ADD COLUMN is_edited TINYINT(1) NOT NULL DEFAULT 0
    AFTER is_internal;
