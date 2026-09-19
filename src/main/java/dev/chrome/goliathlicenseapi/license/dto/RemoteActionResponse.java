package dev.chrome.goliathlicenseapi.license.dto;

import java.time.Instant;
import java.util.UUID;
import dev.chrome.goliathlicenseapi.license.model.RemoteActionStatus;
import dev.chrome.goliathlicenseapi.license.model.RemoteActionType;

public record RemoteActionResponse(
        UUID actionId,
        UUID targetInstanceId,
        RemoteActionType actionType,
        RemoteActionStatus status,
        Instant requestedAt,
        Instant receivedAt,
        Instant completedAt,
        Instant expiresAt,
        String safeFailureReason
) {}
