package dev.chrome.goliathlicenseapi.license.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PluginActionClaimRequest(
    @NotBlank String instanceId,
    @NotBlank String licenseKey
) {}
