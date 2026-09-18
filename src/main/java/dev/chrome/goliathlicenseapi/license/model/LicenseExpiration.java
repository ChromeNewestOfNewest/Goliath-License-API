package dev.chrome.goliathlicenseapi.license.model;

import java.time.Duration;

public enum LicenseExpiration {
    TEN_MINUTES("10m", Duration.ofMinutes(10)),
    ONE_HOUR("1h", Duration.ofHours(1)),
    ONE_DAY("1d", Duration.ofDays(1)),
    FIVE_DAYS("5d", Duration.ofDays(5)),
    TEN_DAYS("10d", Duration.ofDays(10)),
    ONE_MONTH("1mo", Duration.ofDays(30)),
    ONE_YEAR("1y", Duration.ofDays(365)),
    NEVER("never", null);

    private final String value;
    private final Duration duration;

    LicenseExpiration(String value, Duration duration) {
        this.value = value;
        this.duration = duration;
    }

    public String getValue() {
        return value;
    }

    public Duration getDuration() {
        return duration;
    }

    public static LicenseExpiration fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Expiration value is required.");
        }

        for (LicenseExpiration expiration : values()) {
            if (expiration.value.equalsIgnoreCase(value.trim())) {
                return expiration;
            }
        }

        throw new IllegalArgumentException("Unsupported expiration value: " + value);
    }
}
