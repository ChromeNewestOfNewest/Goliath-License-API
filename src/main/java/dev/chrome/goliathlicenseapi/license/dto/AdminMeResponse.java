package dev.chrome.goliathlicenseapi.license.dto;

import java.time.Instant;

public record AdminMeResponse(
        String username,
        String role,
        Instant lastLoginAt
) {}
