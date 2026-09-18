package dev.chrome.goliathlicenseapi.license.dto;

import java.time.Instant;
import java.util.UUID;

public record PluginValidateResponse(
        String code,
        String message,
        UUID licenseId,
        Instant expiresAt
) {}
