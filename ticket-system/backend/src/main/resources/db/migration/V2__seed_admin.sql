-- V2: Seed default admin user
-- Default password: Admin@123
-- bcrypt hash generated with strength 10

INSERT INTO `user` (username, email, phone, password, role, status, created_by, created_date)
VALUES (
    'admin',
    'admin@ticket.local',
    NULL,
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ROLE_ADMIN',
    1,
    0,
    (UNIX_TIMESTAMP(NOW()) * 1000)
);
