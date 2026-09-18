package dev.chrome.goliathlicenseapi.license.exception;

public class LicenseNotFoundException extends RuntimeException {
    public LicenseNotFoundException(String message) {
        super(message);
    }
}
