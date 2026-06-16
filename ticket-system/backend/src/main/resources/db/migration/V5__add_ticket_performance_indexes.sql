-- ============================================================
-- V5: Add composite & covering indexes for 1M+ ticket queries
-- ============================================================
-- Problem: single-column indexes can't serve combined filters
--   e.g. WHERE status='OPEN' AND assigned_to IS NULL only uses
--   ONE index, scanning hundreds of thousands of rows.
-- Solution: composite indexes covering the exact query patterns.
-- ============================================================

-- Workbench "Pending Queue":  WHERE status = ? AND assigned_to IS NULL
CREATE INDEX idx_ticket_status_assigned ON ticket (status, assigned_to);

-- Workbench "My Active":      WHERE assigned_to = ? AND status = ?
CREATE INDEX idx_ticket_assigned_status ON ticket (assigned_to, status);

-- User's own tickets:        WHERE created_by = ? AND status = ?
CREATE INDEX idx_ticket_created_status ON ticket (created_by, status);

-- Sort by created date (existing ORDER BY created_date had no index — filesort on 1M rows)
CREATE INDEX idx_ticket_created_date ON ticket (created_date);
