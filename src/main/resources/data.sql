-- Seed Roles
INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_MANAGER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_USER') ON CONFLICT (name) DO NOTHING;

-- Seed Categories
INSERT INTO categories (name, description) VALUES ('Bug', 'Unexpected system crash, error, or software behavior') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name, description) VALUES ('Feature Request', 'New functionality or enhancement requested by clients') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name, description) VALUES ('Task', 'Routine development task, documentation, or minor tweak') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name, description) VALUES ('IT Support', 'Hardware, networking, accounts, or software installation support') ON CONFLICT (name) DO NOTHING;

-- Seed Priorities
INSERT INTO priorities (name, weight) VALUES ('Low', 1) ON CONFLICT (name) DO NOTHING;
INSERT INTO priorities (name, weight) VALUES ('Medium', 2) ON CONFLICT (name) DO NOTHING;
INSERT INTO priorities (name, weight) VALUES ('High', 3) ON CONFLICT (name) DO NOTHING;
INSERT INTO priorities (name, weight) VALUES ('Critical', 4) ON CONFLICT (name) DO NOTHING;

-- Seed Statuses
INSERT INTO statuses (name, closed_status) VALUES ('Open', false) ON CONFLICT (name) DO NOTHING;
INSERT INTO statuses (name, closed_status) VALUES ('In Progress', false) ON CONFLICT (name) DO NOTHING;
INSERT INTO statuses (name, closed_status) VALUES ('Resolved', true) ON CONFLICT (name) DO NOTHING;
INSERT INTO statuses (name, closed_status) VALUES ('Closed', true) ON CONFLICT (name) DO NOTHING;

-- Seed default Users (password: 'password')
INSERT INTO users (username, email, password, full_name, active, created_at)
VALUES (
    'admin',
    'admin@tracker.com',
    '$2a$10$wKz0bZecy1x2.0QjM8b76.1M/sZzQ0NlG43F.ZzC8i/hVbH5M3eEa',
    'System Administrator',
    true,
    CURRENT_TIMESTAMP
) ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, email, password, full_name, active, created_at)
VALUES (
    'manager',
    'manager@tracker.com',
    '$2a$10$wKz0bZecy1x2.0QjM8b76.1M/sZzQ0NlG43F.ZzC8i/hVbH5M3eEa',
    'Project Manager',
    true,
    CURRENT_TIMESTAMP
) ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, email, password, full_name, active, created_at)
VALUES (
    'developer',
    'developer@tracker.com',
    '$2a$10$wKz0bZecy1x2.0QjM8b76.1M/sZzQ0NlG43F.ZzC8i/hVbH5M3eEa',
    'Software Engineer',
    true,
    CURRENT_TIMESTAMP
) ON CONFLICT (username) DO NOTHING;

-- Map Users to Roles
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'manager' AND r.name = 'ROLE_MANAGER'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'developer' AND r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;
