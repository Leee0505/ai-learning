-- V2: Seed default admin user
-- Default password: Admin@123
-- bcrypt hash generated with strength 10

INSERT INTO `user` (username, email, phone, password, role, status, created_by, created_date)
VALUES (
    'admin',
    'admin@ticket.local',
    NULL,
    '$2b$10$PAoQ1fY8qFNLysbwyfl74e9RTwgm9N2H8s0vSNXqnCbmb3y4wIMKi',
    'ROLE_ADMIN',
    1,
    0,
    (UNIX_TIMESTAMP(NOW()) * 1000)
);
