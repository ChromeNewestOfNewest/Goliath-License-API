package dev.chrome.goliathlicenseapi.license.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "remote_actions")
public class RemoteAction {

    @Id
    @Column(name = "action_id", nullable = false, updatable = false)
    private UUID actionId;

    @Column(name = "target_instance_id", nullable = false)
    private UUID targetInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private RemoteActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RemoteActionStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "safe_failure_reason", length = 1024)
    private String safeFailureReason;

    public UUID getActionId() { return actionId; }
    public void setActionId(UUID actionId) { this.actionId = actionId; }

    public UUID getTargetInstanceId() { return targetInstanceId; }
    public void setTargetInstanceId(UUID targetInstanceId) { this.targetInstanceId = targetInstanceId; }

    public RemoteActionType getActionType() { return actionType; }
    public void setActionType(RemoteActionType actionType) { this.actionType = actionType; }

    public RemoteActionStatus getStatus() { return status; }
    public void setStatus(RemoteActionStatus status) { this.status = status; }

    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public String getSafeFailureReason() { return safeFailureReason; }
    public void setSafeFailureReason(String safeFailureReason) { this.safeFailureReason = safeFailureReason; }
}
