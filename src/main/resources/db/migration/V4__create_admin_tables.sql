-- V4: private admin authentication and audit-trail schema
BEGIN;

CREATE TABLE admin_users (
    id UUID NOT NULL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    last_login_at TIMESTAMPTZ NULL
);

CREATE TABLE admin_audit_logs (
    id UUID NOT NULL PRIMARY KEY,
    admin_username VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    details TEXT NULL,
    ip_address VARCHAR(45) NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_admin_users_username ON admin_users (username);
CREATE INDEX idx_admin_audit_logs_created_at ON admin_audit_logs (created_at);

COMMIT;
