-- ============================================================
-- Multi-Tenant Test Data Generator
-- ============================================================
-- Usage (MySQL 8.0+):
--   mysql -h HOST -u root -p ticket_db < test-data/generate_multi_tenant_data.sql
-- ⚠️ Clears tenant-scoped data first (tenant_id > 1 only)
-- ============================================================

SET SESSION cte_max_recursion_depth = 2000000;

-- ════════════════════════════════════════════════════════════
-- Config
-- ════════════════════════════════════════════════════════════
SET @tenants     = 3;
SET @tickets_per = 200;

-- ════════════════════════════════════════════════════════════
-- Clean
-- ════════════════════════════════════════════════════════════
DELETE FROM notification WHERE tenant_id > 1;
DELETE FROM audit_log WHERE tenant_id > 1;
DELETE FROM ticket_attachment;
DELETE FROM ticket_reply WHERE tenant_id > 1;
DELETE FROM ticket WHERE tenant_id > 1;
DELETE FROM `user` WHERE tenant_id > 1;
DELETE FROM tenant WHERE id > 1;

-- ════════════════════════════════════════════════════════════
-- Tenants
-- ════════════════════════════════════════════════════════════
INSERT INTO tenant (name, slug, status, created_date) VALUES
('Acme Corporation', 'acme', 1, UNIX_TIMESTAMP()*1000),
('Globex Inc.', 'globex', 1, UNIX_TIMESTAMP()*1000),
('Initech', 'initech', 1, UNIX_TIMESTAMP()*1000),
('Umbrella Corp', 'umbrella', 1, UNIX_TIMESTAMP()*1000),
('Wonka Industries', 'wonka', 1, UNIX_TIMESTAMP()*1000);

-- ════════════════════════════════════════════════════════════
-- Users — 1 agent + 3 users per tenant
-- ════════════════════════════════════════════════════════════
INSERT INTO `user` (tenant_id, username, email, password, role, status, created_by, created_date)
SELECT id, CONCAT(slug,'_agent'), CONCAT(slug,'_agent@test.local'),
  '$2b$10$JUAd5ZVNbT.DMuEACW60KewCOZJRGDvHgbzdeJpTU8YCEEZkNaywy',
  'ROLE_AGENT', 1, 0, UNIX_TIMESTAMP()*1000
FROM tenant WHERE id > 1 AND (SELECT @tenants) >= id - 1;

INSERT INTO `user` (tenant_id, username, email, password, role, status, created_by, created_date)
SELECT id, CONCAT(slug,'_',uname), CONCAT(slug,'_',uname,'@test.local'),
  '$2b$10$N4t9sIbuMa2ALsqFTx8CPuYuvXDGihCfeoDXW3li7f8WRKSKAydiK',
  'ROLE_USER', 1, 0, UNIX_TIMESTAMP()*1000
FROM tenant
CROSS JOIN (SELECT 'user1' AS uname UNION ALL SELECT 'user2' UNION ALL SELECT 'user3') u
WHERE tenant.id > 1 AND tenant.id <= @tenants + 1;

-- ════════════════════════════════════════════════════════════
-- Tickets
-- ════════════════════════════════════════════════════════════
SET @t0 = 1747334400000;   -- 2026-05-15
SET @tr = 3024000000;       -- 35 days

INSERT INTO ticket (tenant_id, title, description, status, priority, category,
                    created_by, created_date, last_modified_by, last_modified_date,
                    assigned_to, resolved_date, closed_date)
SELECT
  t.id,
  CONCAT(ELT(1 + CRC32(CONCAT(seq.n, t.id, 't')) % 10,
    'Unable to login to account', 'Password reset not working',
    'Payment processing error', 'Page loading slowly', 'Data export timed out',
    'Email notification not received', 'User profile update failed',
    'File upload size limit exceeded', 'Search functionality broken',
    'Dashboard statistics incorrect'
  ), ' #', seq.n, ' (', t.slug, ')'),

  CONCAT('Auto-generated ticket #', seq.n, ' for ', t.slug, '. ',
    ELT(1 + CRC32(CONCAT(seq.n, t.id, 'd')) % 6,
      'Issue reported via web portal. Affects single user.',
      'Multiple users affected since last deployment.',
      'Intermittent — occurs ~3 out of 5 attempts.',
      'Customer impact: HIGH. Blocking daily operations.',
      'Already tried cache/cookie clear — persists.',
      'Second occurrence this week. Previous fix insufficient.')),

  ELT(1 + CRC32(CONCAT(seq.n, t.id, 's')) % 4, 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'),

  ELT(1 + CRC32(CONCAT(seq.n, t.id, 'p')) % 4, 'LOW', 'MEDIUM', 'HIGH', 'URGENT'),

  ELT(1 + CRC32(CONCAT(seq.n, t.id, 'c')) % 5,
    'BUG', 'FEATURE_REQUEST', 'GENERAL_QUESTION', 'ACCOUNT_ISSUE', 'OTHER'),

  -- created_by: pick a user from this tenant randomly
  (SELECT id FROM `user` WHERE tenant_id = t.id
   ORDER BY CRC32(CONCAT(seq.n, t.id, 'u')) LIMIT 1),

  @t0 + (CRC32(CONCAT(seq.n, t.id, 'ts1')) % @tr),

  -- last_modified_by = same as creator (simplified — use same subquery)
  (SELECT id FROM `user` WHERE tenant_id = t.id
   ORDER BY CRC32(CONCAT(seq.n, t.id, 'u')) LIMIT 1),

  @t0 + (CRC32(CONCAT(seq.n, t.id, 'ts2')) % @tr),

  -- assigned_to: 30% NULL, 70% the agent for this tenant
  CASE WHEN (CRC32(CONCAT(seq.n, t.id, 'a')) % 100) < 30 THEN NULL
       ELSE (SELECT id FROM `user` WHERE tenant_id = t.id AND role = 'ROLE_AGENT' LIMIT 1)
  END,

  -- resolved_date
  CASE
    WHEN ELT(1 + CRC32(CONCAT(seq.n, t.id, 's')) % 4, 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED') IN ('RESOLVED','CLOSED')
    THEN @t0 + (CRC32(CONCAT(seq.n, t.id, 'ts1')) % @tr) + 86400000
    ELSE NULL
  END,

  -- closed_date
  CASE
    WHEN ELT(1 + CRC32(CONCAT(seq.n, t.id, 's')) % 4, 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED') = 'CLOSED'
    THEN @t0 + (CRC32(CONCAT(seq.n, t.id, 'ts1')) % @tr) + 172800000
    ELSE NULL
  END

FROM (
  SELECT tenant_id, id, slug FROM tenant WHERE id > 1 AND id <= @tenants + 1
) t
JOIN (
  WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n < @tickets_per)
  SELECT n FROM seq
) seq;

-- ════════════════════════════════════════════════════════════
-- Summary
-- ════════════════════════════════════════════════════════════
SELECT '=== Tenants ===' AS '';
SELECT id, name, slug FROM tenant ORDER BY id;
SELECT '=== Users ===' AS '';
SELECT tenant_id, username, role FROM `user` ORDER BY tenant_id, role, username;
SELECT '=== Tickets by Tenant ===' AS '';
SELECT tenant_id, status, COUNT(*) AS cnt
FROM ticket WHERE tenant_id > 1
GROUP BY tenant_id, status
ORDER BY tenant_id, FIELD(status,'OPEN','IN_PROGRESS','RESOLVED','CLOSED');
SELECT '=== Total Tickets ===' AS '', COUNT(*) AS t FROM ticket WHERE tenant_id > 1;
