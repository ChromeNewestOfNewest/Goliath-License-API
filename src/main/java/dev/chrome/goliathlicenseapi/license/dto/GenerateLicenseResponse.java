package dev.chrome.goliathlicenseapi.license.dto;

import dev.chrome.goliathlicenseapi.license.model.LicenseStatus;
import java.time.Instant;
import java.util.UUID;

public record GenerateLicenseResponse(
        UUID id,
        String licenseKey,
        String serverIp,
        Integer serverPort,
        String expiration,
        Instant createdAt,
        Instant expiresAt,
        LicenseStatus status
) {
}
