-- Seed Roles
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');
INSERT INTO roles (name) VALUES ('ROLE_MANAGER');
INSERT INTO roles (name) VALUES ('ROLE_USER');

-- Seed Categories
INSERT INTO categories (name, description) VALUES ('Bug', 'Unexpected system crash, error, or software behavior');
INSERT INTO categories (name, description) VALUES ('Feature Request', 'New functionality or enhancement requested by clients');
INSERT INTO categories (name, description) VALUES ('Task', 'Routine development task, documentation, or minor tweak');
INSERT INTO categories (name, description) VALUES ('IT Support', 'Hardware, networking, accounts, or software installation support');

-- Seed Priorities
INSERT INTO priorities (name, weight) VALUES ('Low', 1);
INSERT INTO priorities (name, weight) VALUES ('Medium', 2);
INSERT INTO priorities (name, weight) VALUES ('High', 3);
INSERT INTO priorities (name, weight) VALUES ('Critical', 4);

-- Seed Statuses
INSERT INTO statuses (name, closed_status) VALUES ('Open', false);
INSERT INTO statuses (name, closed_status) VALUES ('In Progress', false);
INSERT INTO statuses (name, closed_status) VALUES ('Resolved', true);
INSERT INTO statuses (name, closed_status) VALUES ('Closed', true);

-- Seed default Users (password: 'password')
INSERT INTO users (username, email, password, full_name, active, created_at)
VALUES (
    'admin',
    'admin@tracker.com',
    '$2a$10$7RwED8W0c5gDWXtZ6UFwKuVL3.5NYa5HObbnOoh0A4wsHLOImsqnK',
    'System Administrator',
    true,
    CURRENT_TIMESTAMP
);

INSERT INTO users (username, email, password, full_name, active, created_at)
VALUES (
    'manager',
    'manager@tracker.com',
    '$2a$10$7RwED8W0c5gDWXtZ6UFwKuVL3.5NYa5HObbnOoh0A4wsHLOImsqnK',
    'Project Manager',
    true,
    CURRENT_TIMESTAMP
);

INSERT INTO users (username, email, password, full_name, active, created_at)
VALUES (
    'developer',
    'developer@tracker.com',
    '$2a$10$7RwED8W0c5gDWXtZ6UFwKuVL3.5NYa5HObbnOoh0A4wsHLOImsqnK',
    'Software Engineer',
    true,
    CURRENT_TIMESTAMP
);

-- Map Users to Roles
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'manager' AND r.name = 'ROLE_MANAGER';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'developer' AND r.name = 'ROLE_USER';
