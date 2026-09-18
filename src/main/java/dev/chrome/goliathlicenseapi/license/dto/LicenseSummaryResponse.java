package dev.chrome.goliathlicenseapi.license.dto;

import dev.chrome.goliathlicenseapi.license.model.LicenseStatus;
import java.time.Instant;
import java.util.UUID;

public record LicenseSummaryResponse(
        UUID id,
        String serverIp,
        Integer serverPort,
        String expiration,
        Instant createdAt,
        Instant expiresAt,
        LicenseStatus status,
        Instant revokedAt,
        String revokedReason
) {
}
