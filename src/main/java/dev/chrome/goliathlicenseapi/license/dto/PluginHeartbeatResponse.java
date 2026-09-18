package dev.chrome.goliathlicenseapi.license.dto;

import java.time.Instant;
import java.util.UUID;

public record PluginHeartbeatResponse(
        String status,
        String message,
        UUID instanceId,
        Instant firstSeen,
        Instant lastSeen,
        String licenseStatus
) {}
