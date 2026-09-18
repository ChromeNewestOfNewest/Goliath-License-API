CREATE TABLE licenses (
    id UUID NOT NULL PRIMARY KEY,
    license_key_hash VARCHAR(255) NOT NULL UNIQUE,
    license_key_salt VARCHAR(255) NOT NULL,
    server_ip VARCHAR(45) NOT NULL,
    server_port INTEGER NOT NULL CHECK (server_port BETWEEN 1 AND 65535),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NULL,
    expiration VARCHAR(20) NOT NULL CHECK (expiration IN ('10m', '1h', '1d', '5d', '10d', '1mo', '1y', 'never')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'REVOKED')),
    revoked_at TIMESTAMPTZ NULL,
    revoked_reason VARCHAR(255) NULL
);

CREATE INDEX idx_licenses_status ON licenses (status);
CREATE INDEX idx_licenses_created_at ON licenses (created_at);
