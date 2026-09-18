package dev.chrome.goliathlicenseapi.license.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;

public record PluginValidateRequest(
        @NotBlank String licenseKey,
        @NotBlank String instanceId,
        @NotNull @Positive @Max(65535) Integer serverPort,
        @NotBlank String goliathVersion,
        @NotBlank String minecraftVersion
) {}
