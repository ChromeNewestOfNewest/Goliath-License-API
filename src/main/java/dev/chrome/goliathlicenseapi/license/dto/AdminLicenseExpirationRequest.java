package dev.chrome.goliathlicenseapi.license.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLicenseExpirationRequest(@NotBlank String expiration) {}
