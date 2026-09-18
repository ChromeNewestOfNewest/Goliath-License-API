package dev.chrome.goliathlicenseapi.license.controller;

import java.time.Instant;

public record ApiError(Instant timestamp, String code, String message) {
}
