-- ============================================================
-- Multi-Tenant Test Data Generator
-- ============================================================
-- USE:  mysql -h HOST -u root -p ticket_db < test-data/generate_multi_tenant_data.sql
-- Safe to re-run — uses INSERT IGNORE for tenants/users, replaces tickets
-- ============================================================

SET SESSION cte_max_recursion_depth = 2000000;
SET @tickets_per = 200;
SET @t0 = 1747334400000;
SET @tr = 3024000000;

-- ════════════════════════════════════════════════════════════
-- Tenants (INSERT IGNORE = skip if already exists)
-- ════════════════════════════════════════════════════════════
INSERT IGNORE INTO tenant (name, slug, status, created_date) VALUES
('Acme Corporation', 'acme', 1, UNIX_TIMESTAMP()*1000),
('Globex Inc.',    'globex', 1, UNIX_TIMESTAMP()*1000),
('Initech',        'initech', 1, UNIX_TIMESTAMP()*1000);

-- ════════════════════════════════════════════════════════════
-- Users — 1 agent + 3 users per generated tenant
-- ════════════════════════════════════════════════════════════
INSERT IGNORE INTO `user` (tenant_id, username, email, password, role, status, created_by, created_date)
SELECT id, CONCAT(slug,'_agent'), CONCAT(slug,'_agent@test.local'),
  '$2b$10$JUAd5ZVNbT.DMuEACW60KewCOZJRGDvHgbzdeJpTU8YCEEZkNaywy',
  'ROLE_AGENT', 1, 0, UNIX_TIMESTAMP()*1000
FROM tenant WHERE id > 1;

INSERT IGNORE INTO `user` (tenant_id, username, email, password, role, status, created_by, created_date)
SELECT t.id, CONCAT(t.slug,'_user', u.n), CONCAT(t.slug,'_user', u.n, '@test.local'),
  '$2b$10$N4t9sIbuMa2ALsqFTx8CPuYuvXDGihCfeoDXW3li7f8WRKSKAydiK',
  'ROLE_USER', 1, 0, UNIX_TIMESTAMP()*1000
FROM tenant t CROSS JOIN (SELECT 1 AS n UNION SELECT 2 UNION SELECT 3) u
WHERE t.id > 1;

-- ════════════════════════════════════════════════════════════
-- Tickets — replace old, generate fresh
-- ════════════════════════════════════════════════════════════
DELETE FROM ticket WHERE tenant_id > 1;

INSERT INTO ticket (tenant_id, title, description, status, priority, category,
                    created_by, created_date, last_modified_by, last_modified_date,
                    assigned_to, resolved_date, closed_date)
SELECT
  t.id,
  CONCAT(ELT(1 + CRC32(CONCAT(seq.n, t.id, 't')) % 10,
    'Unable to login', 'Password reset', 'Payment error',
    'Page slow', 'Export timeout', 'Email missing',
    'Profile broken', 'Upload limit', 'Search broken',
    'Dashboard wrong'
  ), ' #', seq.n, ' (', t.slug, ')'),

  CONCAT('Auto ticket #', seq.n, ' for ', t.slug, '. ',
    ELT(1 + CRC32(CONCAT(seq.n, t.id, 'd')) % 4,
      'Issue reported via web portal.',
      'Multiple users affected since last deployment.',
      'Intermittent — occurs ~3 out of 5 attempts.',
      'Customer impact: HIGH. Blocking daily operations.')),

  ELT(1 + CRC32(CONCAT(seq.n, t.id, 's')) % 4, 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'),
  ELT(1 + CRC32(CONCAT(seq.n, t.id, 'p')) % 4, 'LOW', 'MEDIUM', 'HIGH', 'URGENT'),
  ELT(1 + CRC32(CONCAT(seq.n, t.id, 'c')) % 5, 'BUG', 'FEATURE_REQUEST', 'GENERAL_QUESTION', 'ACCOUNT_ISSUE', 'OTHER'),

  (SELECT id FROM `user` WHERE tenant_id = t.id ORDER BY RAND() LIMIT 1),
  @t0 + (CRC32(CONCAT(seq.n, t.id, 'ts1')) % @tr),
  (SELECT id FROM `user` WHERE tenant_id = t.id ORDER BY RAND() LIMIT 1),
  @t0 + (CRC32(CONCAT(seq.n, t.id, 'ts2')) % @tr),

  CASE WHEN (CRC32(CONCAT(seq.n, t.id, 'a')) % 100) < 30 THEN NULL
       ELSE (SELECT id FROM `user` WHERE tenant_id = t.id AND role = 'ROLE_AGENT' LIMIT 1)
  END,

  CASE WHEN (CRC32(CONCAT(seq.n, t.id, 's')) % 4) IN (2,3)
       THEN @t0 + (CRC32(CONCAT(seq.n, t.id, 'ts1')) % @tr) + 86400000 END,

  CASE WHEN (CRC32(CONCAT(seq.n, t.id, 's')) % 4) = 3
       THEN @t0 + (CRC32(CONCAT(seq.n, t.id, 'ts1')) % @tr) + 172800000 END

FROM tenant t
JOIN (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n < @tickets_per) SELECT n FROM seq) seq
WHERE t.id > 1;

-- ════════════════════════════════════════════════════════════
-- Summary
-- ════════════════════════════════════════════════════════════
SELECT 'Tenants' AS '', id, name, slug FROM tenant ORDER BY id;
SELECT 'Users' AS '', tenant_id, username, role FROM `user` ORDER BY tenant_id, role, username;
SELECT 'Tickets by Tenant' AS '', tenant_id, status, COUNT(*) AS cnt
FROM ticket WHERE tenant_id > 1
GROUP BY tenant_id, status
ORDER BY tenant_id, FIELD(status,'OPEN','IN_PROGRESS','RESOLVED','CLOSED');
SELECT 'Total tickets' AS '', COUNT(*) FROM ticket WHERE tenant_id > 1;
