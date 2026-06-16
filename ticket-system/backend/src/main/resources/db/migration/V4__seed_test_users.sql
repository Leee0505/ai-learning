-- ============================================================
-- V4: Seed Test Users
-- Creates 2 agents and 3 common users for development/testing
-- All passwords are the same as their username suffix:
--   agents: Agent@123 / users: User@123
-- ============================================================

INSERT INTO `user` (username, email, phone, password, role, status, created_date, last_modified_date)
VALUES
('agent1', 'agent1@ticket.local', '13800000001',
 '$2b$10$JUAd5ZVNbT.DMuEACW60KewCOZJRGDvHgbzdeJpTU8YCEEZkNaywy',
 'ROLE_AGENT', 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),

('agent2', 'agent2@ticket.local', '13800000002',
 '$2b$10$JUAd5ZVNbT.DMuEACW60KewCOZJRGDvHgbzdeJpTU8YCEEZkNaywy',
 'ROLE_AGENT', 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),

('user1', 'user1@ticket.local', '13900000001',
 '$2b$10$N4t9sIbuMa2ALsqFTx8CPuYuvXDGihCfeoDXW3li7f8WRKSKAydiK',
 'ROLE_USER', 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),

('user2', 'user2@ticket.local', '13900000002',
 '$2b$10$N4t9sIbuMa2ALsqFTx8CPuYuvXDGihCfeoDXW3li7f8WRKSKAydiK',
 'ROLE_USER', 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),

('user3', 'user3@ticket.local', '13900000003',
 '$2b$10$N4t9sIbuMa2ALsqFTx8CPuYuvXDGihCfeoDXW3li7f8WRKSKAydiK',
 'ROLE_USER', 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);
