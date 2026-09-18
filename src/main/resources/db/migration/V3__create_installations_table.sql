-- V3: Create installations table for tracking Goliath plugin instances
BEGIN;

CREATE TABLE installations (
    instance_id UUID NOT NULL PRIMARY KEY,
    observed_ip VARCHAR(45) NOT NULL,
    server_port INTEGER NOT NULL CHECK (server_port BETWEEN 1 AND 65535),
    goliath_version VARCHAR(100) NOT NULL,
    minecraft_version VARCHAR(100) NOT NULL,
    first_seen TIMESTAMPTZ NOT NULL,
    last_seen TIMESTAMPTZ NOT NULL,
    last_successful_validation TIMESTAMPTZ NULL,
    license_status VARCHAR(20) NOT NULL,
    associated_license_id UUID NULL,
    CONSTRAINT fk_installations_license FOREIGN KEY (associated_license_id) REFERENCES licenses(id) ON DELETE SET NULL
);

CREATE INDEX idx_installations_status ON installations (license_status);
CREATE INDEX idx_installations_last_seen ON installations (last_seen);

COMMIT;
