package dev.chrome.goliathlicenseapi.license.dto;

import jakarta.validation.constraints.NotBlank;

public record PluginHeartbeatRequest(
        @NotBlank String instanceId,
        @NotBlank String goliathVersion,
        @NotBlank String minecraftVersion
) {}
