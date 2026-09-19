-- V6: Add password_changed_at to admin_users to support session invalidation after password change
BEGIN;

ALTER TABLE admin_users ADD COLUMN password_changed_at TIMESTAMPTZ NULL;

COMMIT;