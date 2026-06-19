-- ============================================================
-- V12: Multi-Tenant Test Data
-- Creates 3 tenants with users, tickets, and replies
-- Passwords (BCrypt):
--   Admin@123  →  $2b$10$PAoQ1fY8qFNLysbwyfl74e9RTwgm9N2H8s0vSNXqnCbmb3y4wIMKi
--   Agent@123  →  $2b$10$JUAd5ZVNbT.DMuEACW60KewCOZJRGDvHgbzdeJpTU8YCEEZkNaywy
--   User@123   →  $2b$10$N4t9sIbuMa2ALsqFTx8CPuYuvXDGihCfeoDXW3li7f8WRKSKAydiK
-- ============================================================

SET @now_ms = UNIX_TIMESTAMP() * 1000;

-- ============================================================
-- Tenants
-- ============================================================
INSERT INTO tenant (id, name, slug, status, created_date) VALUES
(2, 'Acme Corporation', 'acme', 1, @now_ms),
(3, 'Globex Inc.', 'globex', 1, @now_ms),
(4, 'Initech', 'initech', 1, @now_ms);

-- ============================================================
-- Users — per tenant
-- ============================================================

-- ── Tenant 2: Acme ──
INSERT INTO `user` (tenant_id, username, email, phone, password, role, status, created_by, created_date) VALUES
(2, 'acme_admin', 'admin@acme.local', NULL,
 '$2b$10$PAoQ1fY8qFNLysbwyfl74e9RTwgm9N2H8s0vSNXqnCbmb3y4wIMKi',
 'ROLE_ADMIN', 1, 0, @now_ms),
(2, 'acme_agent', 'agent@acme.local', NULL,
 '$2b$10$JUAd5ZVNbT.DMuEACW60KewCOZJRGDvHgbzdeJpTU8YCEEZkNaywy',
 'ROLE_AGENT', 1, 0, @now_ms),
(2, 'alice', 'alice@acme.local', NULL,
 '$2b$10$N4t9sIbuMa2ALsqFTx8CPuYuvXDGihCfeoDXW3li7f8WRKSKAydiK',
 'ROLE_USER', 1, 0, @now_ms),
(2, 'bob', 'bob@acme.local', NULL,
 '$2b$10$N4t9sIbuMa2ALsqFTx8CPuYuvXDGihCfeoDXW3li7f8WRKSKAydiK',
 'ROLE_USER', 1, 0, @now_ms);

-- ── Tenant 3: Globex ──
INSERT INTO `user` (tenant_id, username, email, phone, password, role, status, created_by, created_date) VALUES
(3, 'globex_admin', 'admin@globex.local', NULL,
 '$2b$10$PAoQ1fY8qFNLysbwyfl74e9RTwgm9N2H8s0vSNXqnCbmb3y4wIMKi',
 'ROLE_ADMIN', 1, 0, @now_ms),
(3, 'globex_agent', 'agent@globex.local', NULL,
 '$2b$10$JUAd5ZVNbT.DMuEACW60KewCOZJRGDvHgbzdeJpTU8YCEEZkNaywy',
 'ROLE_AGENT', 1, 0, @now_ms),
(3, 'charlie', 'charlie@globex.local', NULL,
 '$2b$10$N4t9sIbuMa2ALsqFTx8CPuYuvXDGihCfeoDXW3li7f8WRKSKAydiK',
 'ROLE_USER', 1, 0, @now_ms),
(3, 'diana', 'diana@globex.local', NULL,
 '$2b$10$N4t9sIbuMa2ALsqFTx8CPuYuvXDGihCfeoDXW3li7f8WRKSKAydiK',
 'ROLE_USER', 1, 0, @now_ms);

-- ── Tenant 4: Initech ──
INSERT INTO `user` (tenant_id, username, email, phone, password, role, status, created_by, created_date) VALUES
(4, 'initech_admin', 'admin@initech.local', NULL,
 '$2b$10$PAoQ1fY8qFNLysbwyfl74e9RTwgm9N2H8s0vSNXqnCbmb3y4wIMKi',
 'ROLE_ADMIN', 1, 0, @now_ms),
(4, 'initech_agent', 'agent@initech.local', NULL,
 '$2b$10$JUAd5ZVNbT.DMuEACW60KewCOZJRGDvHgbzdeJpTU8YCEEZkNaywy',
 'ROLE_AGENT', 1, 0, @now_ms),
(4, 'eve', 'eve@initech.local', NULL,
 '$2b$10$N4t9sIbuMa2ALsqFTx8CPuYuvXDGihCfeoDXW3li7f8WRKSKAydiK',
 'ROLE_USER', 1, 0, @now_ms),
(4, 'frank', 'frank@initech.local', NULL,
 '$2b$10$N4t9sIbuMa2ALsqFTx8CPuYuvXDGihCfeoDXW3li7f8WRKSKAydiK',
 'ROLE_USER', 1, 0, @now_ms);

-- ============================================================
-- Function: get user id by username
-- ============================================================
-- Use subqueries inline since MySQL doesn't support variables in a single session easily

-- ============================================================
-- Tickets — Acme (tenant 2)
-- ============================================================
INSERT INTO `ticket` (tenant_id, title, description, status, priority, category, assigned_to, created_by, created_date) VALUES
(2, 'Cannot log in to the dashboard', 'I keep getting "Invalid credentials" even after resetting my password.', 'OPEN', 'HIGH', 'ACCOUNT_ISSUE', NULL, (SELECT id FROM user WHERE username='alice'), @now_ms - 86400000),
(2, 'Export to CSV is missing columns', 'The Excel export only shows 3 columns but we need all fields.', 'IN_PROGRESS', 'MEDIUM', 'BUG', (SELECT id FROM user WHERE username='acme_agent'), (SELECT id FROM user WHERE username='bob'), @now_ms - 172800000),
(2, 'Add dark mode support', 'Our team works at night and the bright UI is causing eye strain.', 'OPEN', 'LOW', 'FEATURE_REQUEST', NULL, (SELECT id FROM user WHERE username='alice'), @now_ms - 43200000),
(2, 'API rate limit too low for integration', 'We are hitting the 10/min limit with our automated tooling. Need 100/min.', 'RESOLVED', 'URGENT', 'OTHER', (SELECT id FROM user WHERE username='acme_agent'), (SELECT id FROM user WHERE username='bob'), @now_ms - 604800000);

-- ============================================================
-- Tickets — Globex (tenant 3)
-- ============================================================
INSERT INTO `ticket` (tenant_id, title, description, status, priority, category, assigned_to, created_by, created_date) VALUES
(3, 'Profile picture upload fails', 'Every time I try to upload a PNG it says "file type not allowed". But PNG is in the allowlist.', 'OPEN', 'MEDIUM', 'BUG', NULL, (SELECT id FROM user WHERE username='charlie'), @now_ms - 3600000),
(3, 'Need bulk user import', 'We have 200 employees to onboard. Manual creation is not viable.', 'IN_PROGRESS', 'HIGH', 'FEATURE_REQUEST', (SELECT id FROM user WHERE username='globex_agent'), (SELECT id FROM user WHERE username='diana'), @now_ms - 86400000),
(3, 'Ticket #5 assigned to wrong agent', 'This was supposed to go to globex_agent but went to default admin.', 'CLOSED', 'LOW', 'GENERAL_QUESTION', NULL, (SELECT id FROM user WHERE username='charlie'), @now_ms - 259200000);

-- ============================================================
-- Tickets — Initech (tenant 4)
-- ============================================================
INSERT INTO `ticket` (tenant_id, title, description, status, priority, category, assigned_to, created_by, created_date) VALUES
(4, 'Urgent: Production outage — payment gateway down', 'Customers cannot complete checkout. Payment API returns 502. This is blocking revenue.', 'IN_PROGRESS', 'URGENT', 'BUG', (SELECT id FROM user WHERE username='initech_agent'), (SELECT id FROM user WHERE username='eve'), @now_ms - 7200000),
(4, 'SLA configuration should allow minutes not just hours', 'We need 15-minute granularity for our critical tickets.', 'RESOLVED', 'MEDIUM', 'FEATURE_REQUEST', (SELECT id FROM user WHERE username='initech_agent'), (SELECT id FROM user WHERE username='frank'), @now_ms - 1209600000),
(4, 'How do I reset my password?', 'I cannot find the password reset option in the profile page.', 'CLOSED', 'LOW', 'GENERAL_QUESTION', NULL, (SELECT id FROM user WHERE username='eve'), @now_ms - 1814400000);

-- ============================================================
-- Replies — for IN_PROGRESS tickets (tenant 2, 3, 4)
-- ============================================================
INSERT INTO `ticket_reply` (tenant_id, ticket_id, user_id, content, is_internal, created_by, created_date) VALUES
-- Acme ticket #2 (IN_PROGRESS, assigned to acme_agent)
(2, (SELECT id FROM ticket WHERE title='Export to CSV is missing columns' AND tenant_id=2),
 (SELECT id FROM user WHERE username='acme_agent'), 'Thanks for reporting this. I can reproduce the issue — looking into it now.', 0, (SELECT id FROM user WHERE username='acme_agent'), @now_ms - 100000000),
-- Acme ticket #2 internal note
(2, (SELECT id FROM ticket WHERE title='Export to CSV is missing columns' AND tenant_id=2),
 (SELECT id FROM user WHERE username='acme_agent'), 'Need to check POI column mapping — might be a regression from the custom fields feature.', 1, (SELECT id FROM user WHERE username='acme_agent'), @now_ms - 90000000),

-- Globex ticket #6 (IN_PROGRESS, assigned to globex_agent)
(3, (SELECT id FROM ticket WHERE title='Need bulk user import' AND tenant_id=3),
 (SELECT id FROM user WHERE username='globex_agent'), 'We are planning this for v1.2. For now I can help import via SQL if you send the list.', 0, (SELECT id FROM user WHERE username='globex_agent'), @now_ms - 40000000),

-- Initech ticket #8 (IN_PROGRESS, urgent)
(4, (SELECT id FROM ticket WHERE title LIKE 'Urgent: Production outage%' AND tenant_id=4),
 (SELECT id FROM user WHERE username='initech_agent'), 'Investigating — payment API team has been paged. This appears to be a gateway timeout on their side.', 0, (SELECT id FROM user WHERE username='initech_agent'), @now_ms - 3000000),
(4, (SELECT id FROM ticket WHERE title LIKE 'Urgent: Production outage%' AND tenant_id=4),
 (SELECT id FROM user WHERE username='initech_agent'), 'Payment gateway recovered at 14:32. Root cause: upstream DNS failure. Monitoring for 30min before resolving.', 0, (SELECT id FROM user WHERE username='initech_agent'), @now_ms - 1000000);

-- ============================================================
-- Notifications — for assigned tickets
-- ============================================================
INSERT INTO `notification` (tenant_id, user_id, type, ticket_id, title, is_read, created_date) VALUES
(2, (SELECT id FROM user WHERE username='acme_agent'), 'TICKET_ASSIGNED', (SELECT id FROM ticket WHERE title='Export to CSV is missing columns' AND tenant_id=2), 'Ticket assigned to you', 1, @now_ms - 172000000),
(3, (SELECT id FROM user WHERE username='globex_agent'), 'TICKET_ASSIGNED', (SELECT id FROM ticket WHERE title='Need bulk user import' AND tenant_id=3), 'Ticket assigned to you', 0, @now_ms - 86000000),
(4, (SELECT id FROM user WHERE username='initech_agent'), 'TICKET_ASSIGNED', (SELECT id FROM ticket WHERE title LIKE 'Urgent: Production outage%' AND tenant_id=4), 'Ticket assigned to you', 0, @now_ms - 7000000);

-- ============================================================
-- Audit log entries
-- ============================================================
INSERT INTO `audit_log` (tenant_id, user_id, action, target_type, target_id, detail, created_by, created_date) VALUES
(2, (SELECT id FROM user WHERE username='alice'), 'CREATE', 'ticket', (SELECT id FROM ticket WHERE title='Cannot log in to the dashboard' AND tenant_id=2), 'Ticket created by alice', (SELECT id FROM user WHERE username='alice'), @now_ms - 86400000),
(2, (SELECT id FROM user WHERE username='bob'), 'CREATE', 'ticket', (SELECT id FROM ticket WHERE title='Export to CSV is missing columns' AND tenant_id=2), 'Ticket created by bob', (SELECT id FROM user WHERE username='bob'), @now_ms - 172800000),
(4, (SELECT id FROM user WHERE username='eve'), 'CREATE', 'ticket', (SELECT id FROM ticket WHERE title LIKE 'Urgent: Production outage%' AND tenant_id=4), 'Ticket created by eve', (SELECT id FROM user WHERE username='eve'), @now_ms - 7200000);
