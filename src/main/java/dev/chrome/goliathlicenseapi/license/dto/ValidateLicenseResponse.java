package dev.chrome.goliathlicenseapi.license.dto;

import dev.chrome.goliathlicenseapi.license.model.LicenseStatus;
import java.util.UUID;

public record ValidateLicenseResponse(
        boolean valid,
        String code,
        String message,
        UUID licenseId,
        String serverIp,
        Integer serverPort,
        String expiration,
        LicenseStatus status
) {
}
