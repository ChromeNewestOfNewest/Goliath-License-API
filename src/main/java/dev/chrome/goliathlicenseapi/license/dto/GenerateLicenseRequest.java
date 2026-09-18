package dev.chrome.goliathlicenseapi.license.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GenerateLicenseRequest(
        @NotBlank(message = "Server IP is required.") String serverIp,
        @NotNull(message = "Port is required.") @Min(value = 1, message = "Port must be between 1 and 65535.") @Max(value = 65535, message = "Port must be between 1 and 65535.") Integer serverPort,
        @NotBlank(message = "Expiration is required.") String expiration
) {
}
