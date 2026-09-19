-- Create table for remote server actions
CREATE TABLE IF NOT EXISTS remote_actions (
    action_id UUID PRIMARY KEY,
    target_instance_id UUID NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    requested_by VARCHAR(100) NOT NULL,
    received_at TIMESTAMP WITHOUT TIME ZONE NULL,
    completed_at TIMESTAMP WITHOUT TIME ZONE NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NULL,
    safe_failure_reason VARCHAR(1024) NULL
);

CREATE INDEX IF NOT EXISTS idx_remote_actions_target ON remote_actions (target_instance_id);
CREATE INDEX IF NOT EXISTS idx_remote_actions_status ON remote_actions (status);
