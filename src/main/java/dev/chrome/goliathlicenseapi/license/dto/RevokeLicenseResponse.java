package dev.chrome.goliathlicenseapi.license.dto;

import dev.chrome.goliathlicenseapi.license.model.LicenseStatus;
import java.time.Instant;
import java.util.UUID;

public record RevokeLicenseResponse(
        boolean revoked,
        UUID licenseId,
        String message,
        LicenseStatus status,
        Instant revokedAt,
        String revokedReason
) {
}
