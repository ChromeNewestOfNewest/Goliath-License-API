-- V5: Add blocking support for installations
BEGIN;

ALTER TABLE installations
    ADD COLUMN blocked_at TIMESTAMPTZ NULL,
    ADD COLUMN blocked_reason VARCHAR(255) NULL;

CREATE INDEX idx_installations_blocked_at ON installations (blocked_at);

COMMIT;