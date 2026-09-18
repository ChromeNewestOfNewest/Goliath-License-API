package dev.chrome.goliathlicenseapi.license.dto;

import jakarta.validation.constraints.NotBlank;

public record RevokeLicenseRequest(
        @NotBlank(message = "License key is required.") String licenseKey,
        String reason
) {
}
