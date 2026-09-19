package dev.chrome.goliathlicenseapi.license.dto;

import java.time.Instant;
import java.util.UUID;
import dev.chrome.goliathlicenseapi.license.model.RemoteActionStatus;
import dev.chrome.goliathlicenseapi.license.model.RemoteActionType;

public record PluginActionPollResponse(
    boolean hasAction,
    UUID actionId,
    RemoteActionType actionType,
    Instant requestedAt,
    Instant expiresAt
) {}
