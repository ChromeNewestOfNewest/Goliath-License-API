package dev.chrome.goliathlicenseapi.license.dto;

public record AdminLoginResponse(
        String status,
        String username,
        String message
) {}
