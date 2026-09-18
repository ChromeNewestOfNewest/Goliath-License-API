package dev.chrome.goliathlicenseapi.license.dto;

import dev.chrome.goliathlicenseapi.license.model.LicenseExpiration;
import dev.chrome.goliathlicenseapi.license.model.LicenseStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminLicenseSummary(
        UUID id,
        String serverIp,
        Integer serverPort,
        LicenseExpiration expiration,
        String expirationValue,
        Instant createdAt,
        Instant expiresAt,
        LicenseStatus status,
        Instant revokedAt,
        String revokedReason
) {}
