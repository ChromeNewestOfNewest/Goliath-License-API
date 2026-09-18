package dev.chrome.goliathlicenseapi.license.service;

import dev.chrome.goliathlicenseapi.license.dto.GenerateLicenseRequest;
import dev.chrome.goliathlicenseapi.license.dto.GenerateLicenseResponse;
import dev.chrome.goliathlicenseapi.license.dto.LicenseSummaryResponse;
import dev.chrome.goliathlicenseapi.license.dto.RevokeLicenseRequest;
import dev.chrome.goliathlicenseapi.license.dto.RevokeLicenseResponse;
import dev.chrome.goliathlicenseapi.license.dto.ValidateLicenseRequest;
import dev.chrome.goliathlicenseapi.license.dto.ValidateLicenseResponse;
import dev.chrome.goliathlicenseapi.license.exception.InvalidLicenseArgumentException;
import dev.chrome.goliathlicenseapi.license.exception.LicenseNotFoundException;
import dev.chrome.goliathlicenseapi.license.model.License;
import dev.chrome.goliathlicenseapi.license.model.LicenseExpiration;
import dev.chrome.goliathlicenseapi.license.model.LicenseStatus;
import dev.chrome.goliathlicenseapi.license.repository.LicenseRepository;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LicenseService {

    private static final String LICENSE_PREFIX = "GOLIATH";
    private static final String LICENSE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final LicenseRepository licenseRepository;
    private final Clock clock;

    public LicenseService(LicenseRepository licenseRepository) {
        this.licenseRepository = licenseRepository;
        this.clock = Clock.systemUTC();
    }

    public GenerateLicenseResponse generateLicense(GenerateLicenseRequest request) {
        String normalizedIp = validateServerIp(request.serverIp());
        Integer serverPort = validatePort(request.serverPort());
        LicenseExpiration expiration = LicenseExpiration.fromValue(request.expiration());

        String plainKey = buildLicenseKey();
        String salt = generateSalt();
        String hash = hashKey(plainKey, salt);

        for (int attempt = 0; attempt < 10; attempt++) {
            final String candidateHash = hash;
            if (licenseRepository.findAll().stream().noneMatch(license -> Objects.equals(license.getLicenseKeyHash(), candidateHash))) {
                break;
            }
            plainKey = buildLicenseKey();
            salt = generateSalt();
            hash = hashKey(plainKey, salt);
        }

        Instant now = Instant.now(clock);
        License license = new License();
        license.setLicenseKeyHash(hash);
        license.setLicenseKeySalt(salt);
        license.setServerIp(normalizedIp);
        license.setServerPort(serverPort);
        license.setExpiration(expiration);
        license.setStatus(LicenseStatus.ACTIVE);
        license.setCreatedAt(now);
        license.setExpiresAt(expiration == LicenseExpiration.NEVER ? null : now.plus(expiration.getDuration()));

        License saved = licenseRepository.save(license);

        return new GenerateLicenseResponse(
                saved.getId(),
                plainKey,
                saved.getServerIp(),
                saved.getServerPort(),
                saved.getExpiration().getValue(),
                saved.getCreatedAt(),
                saved.getExpiresAt(),
                saved.getStatus()
        );
    }

    public ValidateLicenseResponse validateLicense(ValidateLicenseRequest request) {
        String normalizedIp = validateServerIp(request.serverIp());
        Integer serverPort = validatePort(request.serverPort());
        License license = findMatchingLicense(request.licenseKey());

        if (license == null) {
            return new ValidateLicenseResponse(
                    false,
                    "INVALID_KEY",
                    "License key is invalid.",
                    null,
                    normalizedIp,
                    serverPort,
                    null,
                    null
            );
        }

        if (license.getStatus() == LicenseStatus.REVOKED) {
            return new ValidateLicenseResponse(
                    false,
                    "REVOKED",
                    "License has been revoked.",
                    license.getId(),
                    license.getServerIp(),
                    license.getServerPort(),
                    license.getExpiration().getValue(),
                    license.getStatus()
            );
        }

        if (!Objects.equals(license.getServerIp(), normalizedIp)) {
            return new ValidateLicenseResponse(
                    false,
                    "WRONG_IP",
                    "License does not match the server IP.",
                    license.getId(),
                    license.getServerIp(),
                    license.getServerPort(),
                    license.getExpiration().getValue(),
                    license.getStatus()
            );
        }

        if (!Objects.equals(license.getServerPort(), serverPort)) {
            return new ValidateLicenseResponse(
                    false,
                    "WRONG_PORT",
                    "License does not match the server port.",
                    license.getId(),
                    license.getServerIp(),
                    license.getServerPort(),
                    license.getExpiration().getValue(),
                    license.getStatus()
            );
        }

        if (license.isExpired(Instant.now(clock))) {
            return new ValidateLicenseResponse(
                    false,
                    "EXPIRED",
                    "License has expired.",
                    license.getId(),
                    license.getServerIp(),
                    license.getServerPort(),
                    license.getExpiration().getValue(),
                    license.getStatus()
            );
        }

        return new ValidateLicenseResponse(
                true,
                "VALID",
                "License is valid.",
                license.getId(),
                license.getServerIp(),
                license.getServerPort(),
                license.getExpiration().getValue(),
                license.getStatus()
        );
    }

    public RevokeLicenseResponse revokeLicense(RevokeLicenseRequest request) {
        License license = findMatchingLicense(request.licenseKey());
        if (license == null) {
            throw new LicenseNotFoundException("License key was not found.");
        }

        if (license.getStatus() == LicenseStatus.REVOKED) {
            return new RevokeLicenseResponse(
                    true,
                    license.getId(),
                    "License is already revoked.",
                    license.getStatus(),
                    license.getRevokedAt(),
                    license.getRevokedReason()
            );
        }

        String reason = (request.reason() == null || request.reason().isBlank())
                ? "Revoked by administrator."
                : request.reason().trim();

        license.setStatus(LicenseStatus.REVOKED);
        license.setRevokedAt(Instant.now(clock));
        license.setRevokedReason(reason);
        licenseRepository.save(license);

        return new RevokeLicenseResponse(
                true,
                license.getId(),
                "License revoked successfully.",
                LicenseStatus.REVOKED,
                license.getRevokedAt(),
                reason
        );
    }

    public List<LicenseSummaryResponse> listLicenses() {
        return licenseRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    public License findMatchingLicense(String plainKey) {
        if (plainKey == null || plainKey.isBlank()) {
            throw new InvalidLicenseArgumentException("License key is required.");
        }

        String normalizedKey = plainKey.trim();
        for (License license : licenseRepository.findAll()) {
            if (matchesKey(normalizedKey, license)) {
                return license;
            }
        }
        return null;
    }

    private boolean matchesKey(String plainKey, License license) {
        return Objects.equals(hashKey(plainKey, license.getLicenseKeySalt()), license.getLicenseKeyHash());
    }

    private String validateServerIp(String rawIp) {
        if (rawIp == null || rawIp.isBlank()) {
            throw new InvalidLicenseArgumentException("Server IP is required.");
        }

        String candidate = rawIp.trim();
        if (candidate.equalsIgnoreCase("localhost") || candidate.contains(" ")) {
            throw new InvalidLicenseArgumentException("Server IP must be a numerical IPv4 or IPv6 address.");
        }

        if (candidate.contains(":")) {
            if (!candidate.matches("^[0-9A-Fa-f:.]+$")) {
                throw new InvalidLicenseArgumentException("Server IP must be a numerical IPv4 or IPv6 address.");
            }
            try {
                return InetAddress.getByName(candidate).getHostAddress();
            } catch (UnknownHostException ex) {
                throw new InvalidLicenseArgumentException("Server IP must be a numerical IPv4 or IPv6 address.");
            }
        }

        if (!candidate.matches("^(?:\\d{1,3}\\.){3}\\d{1,3}$")) {
            throw new InvalidLicenseArgumentException("Server IP must be a numerical IPv4 or IPv6 address.");
        }

        String[] octets = candidate.split("\\.");
        for (String octet : octets) {
            int value;
            try {
                value = Integer.parseInt(octet);
            } catch (NumberFormatException ex) {
                throw new InvalidLicenseArgumentException("Server IP must be a numerical IPv4 or IPv6 address.");
            }
            if (value < 0 || value > 255) {
                throw new InvalidLicenseArgumentException("Server IP must be a valid IPv4 address.");
            }
        }

        return candidate;
    }

    private Integer validatePort(Integer port) {
        if (port == null) {
            throw new InvalidLicenseArgumentException("Port is required.");
        }
        if (port < 1 || port > 65535) {
            throw new InvalidLicenseArgumentException("Port must be between 1 and 65535.");
        }
        return port;
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashKey(String plainKey, String salt) {
        try {
            byte[] decodedSalt = Base64.getDecoder().decode(salt);
            PBEKeySpec spec = new PBEKeySpec(plainKey.toCharArray(), decodedSalt, 120_000, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Failure while hashing license key.", ex);
        }
    }

    private String buildLicenseKey() {
        Random random = new SecureRandom();
        StringBuilder key = new StringBuilder(LICENSE_PREFIX);
        key.append('-');

        for (int group = 0; group < 4; group++) {
            if (group > 0) {
                key.append('-');
            }
            for (int index = 0; index < 4; index++) {
                key.append(LICENSE_ALPHABET.charAt(random.nextInt(LICENSE_ALPHABET.length())));
            }
        }

        return key.toString();
    }

    private LicenseSummaryResponse toSummary(License license) {
        return new LicenseSummaryResponse(
                license.getId(),
                license.getServerIp(),
                license.getServerPort(),
                license.getExpiration().getValue(),
                license.getCreatedAt(),
                license.getExpiresAt(),
                license.getStatus(),
                license.getRevokedAt(),
                license.getRevokedReason()
        );
    }
}
