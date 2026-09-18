package dev.chrome.goliathlicenseapi.license.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import dev.chrome.goliathlicenseapi.license.dto.GenerateLicenseRequest;
import dev.chrome.goliathlicenseapi.license.dto.GenerateLicenseResponse;
import dev.chrome.goliathlicenseapi.license.dto.ValidateLicenseRequest;
import dev.chrome.goliathlicenseapi.license.dto.ValidateLicenseResponse;
import dev.chrome.goliathlicenseapi.license.model.License;
import dev.chrome.goliathlicenseapi.license.model.LicenseExpiration;
import dev.chrome.goliathlicenseapi.license.model.LicenseStatus;
import dev.chrome.goliathlicenseapi.license.repository.LicenseRepository;
import dev.chrome.goliathlicenseapi.license.service.LicenseService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LicenseControllerIntegrationTests {

    @Mock
    private LicenseRepository licenseRepository;

    private LicenseService licenseService;

    @BeforeEach
    void setUp() {
        licenseService = new LicenseService(licenseRepository);
    }

    @Test
    void validLicensePassesValidation() {
        LicenseFixture fixture = generateLicenseFixture("127.0.0.1", 25565, "1d");
        when(licenseRepository.findAll()).thenReturn(List.of(fixture.license()));

        ValidateLicenseResponse response = licenseService.validateLicense(new ValidateLicenseRequest(
                fixture.response().licenseKey(),
                "127.0.0.1",
                25565));

        assertThat(response.valid()).isTrue();
        assertThat(response.code()).isEqualTo("VALID");
    }

    @Test
    void invalidLicenseKeyFailsValidation() {
        LicenseFixture fixture = generateLicenseFixture("127.0.0.1", 25565, "1d");
        when(licenseRepository.findAll()).thenReturn(List.of(fixture.license()));

        ValidateLicenseResponse response = licenseService.validateLicense(new ValidateLicenseRequest(
                "GOLIATH-AAAA-BBBB-CCCC-DDDD",
                "127.0.0.1",
                25565));

        assertThat(response.valid()).isFalse();
        assertThat(response.code()).isEqualTo("INVALID_KEY");
    }

    @Test
    void expiredLicenseFailsValidation() {
        LicenseFixture fixture = generateLicenseFixture("127.0.0.1", 25565, "1d");
        License expired = fixture.license();
        expired.setExpiresAt(Instant.now().minusSeconds(60));
        when(licenseRepository.findAll()).thenReturn(List.of(expired));

        ValidateLicenseResponse response = licenseService.validateLicense(new ValidateLicenseRequest(
                fixture.response().licenseKey(),
                "127.0.0.1",
                25565));

        assertThat(response.valid()).isFalse();
        assertThat(response.code()).isEqualTo("EXPIRED");
    }

    @Test
    void revokedLicenseFailsValidation() {
        LicenseFixture fixture = generateLicenseFixture("127.0.0.1", 25565, "1d");
        License revoked = fixture.license();
        revoked.setStatus(LicenseStatus.REVOKED);
        revoked.setRevokedAt(Instant.now());
        revoked.setRevokedReason("manual revocation");
        when(licenseRepository.findAll()).thenReturn(List.of(revoked));

        ValidateLicenseResponse response = licenseService.validateLicense(new ValidateLicenseRequest(
                fixture.response().licenseKey(),
                "127.0.0.1",
                25565));

        assertThat(response.valid()).isFalse();
        assertThat(response.code()).isEqualTo("REVOKED");
    }

    @Test
    void wrongIpFailsValidation() {
        LicenseFixture fixture = generateLicenseFixture("127.0.0.1", 25565, "1d");
        when(licenseRepository.findAll()).thenReturn(List.of(fixture.license()));

        ValidateLicenseResponse response = licenseService.validateLicense(new ValidateLicenseRequest(
                fixture.response().licenseKey(),
                "127.0.0.2",
                25565));

        assertThat(response.valid()).isFalse();
        assertThat(response.code()).isEqualTo("WRONG_IP");
    }

    @Test
    void wrongPortFailsValidation() {
        LicenseFixture fixture = generateLicenseFixture("127.0.0.1", 25565, "1d");
        when(licenseRepository.findAll()).thenReturn(List.of(fixture.license()));

        ValidateLicenseResponse response = licenseService.validateLicense(new ValidateLicenseRequest(
                fixture.response().licenseKey(),
                "127.0.0.1",
                25566));

        assertThat(response.valid()).isFalse();
        assertThat(response.code()).isEqualTo("WRONG_PORT");
    }

    @Test
    void generateSupportsAllExpirationValues() {
        String[] expirations = {"10m","1h","1d","5d","10d","1mo","1y","never"};
        for (String exp : expirations) {
            LicenseFixture fixture = generateLicenseFixture("127.0.0.1", 25565, exp);
            // user-facing expiration value should match the input
            assertThat(fixture.response().expiration()).isEqualTo(exp);
            if ("never".equals(exp)) {
                assertThat(fixture.license().getExpiresAt()).isNull();
            } else {
                assertThat(fixture.license().getExpiresAt()).isNotNull();
            }
        }
    }

    private LicenseFixture generateLicenseFixture(String ip, int port, String expiration) {
        AtomicReference<License> savedLicenseRef = new AtomicReference<>();
        when(licenseRepository.findAll()).thenReturn(List.of());
        when(licenseRepository.save(any(License.class))).thenAnswer(invocation -> {
            License saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            savedLicenseRef.set(saved);
            return saved;
        });

        GenerateLicenseResponse response = licenseService.generateLicense(new GenerateLicenseRequest(ip, port, expiration));
        return new LicenseFixture(response, savedLicenseRef.get());
    }

    private record LicenseFixture(GenerateLicenseResponse response, License license) {
    }
}
