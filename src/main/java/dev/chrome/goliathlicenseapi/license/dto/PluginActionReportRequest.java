package dev.chrome.goliathlicenseapi.license.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PluginActionReportRequest(
    @NotBlank String instanceId,
    @NotBlank String licenseKey,
    @NotNull UUID actionId,
    @NotBlank String result,
    String failureReason
) {}
