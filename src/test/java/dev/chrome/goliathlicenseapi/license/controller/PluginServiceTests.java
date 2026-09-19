package dev.chrome.goliathlicenseapi.license.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.chrome.goliathlicenseapi.license.dto.PluginHeartbeatRequest;
import dev.chrome.goliathlicenseapi.license.dto.PluginValidateRequest;
import dev.chrome.goliathlicenseapi.license.model.Installation;
import dev.chrome.goliathlicenseapi.license.model.InstallationStatus;
import dev.chrome.goliathlicenseapi.license.model.License;
import dev.chrome.goliathlicenseapi.license.model.LicenseExpiration;
import dev.chrome.goliathlicenseapi.license.model.LicenseStatus;
import dev.chrome.goliathlicenseapi.license.repository.InstallationRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import dev.chrome.goliathlicenseapi.license.service.RequestRateLimiter;

@ExtendWith(MockitoExtension.class)
class PluginServiceTests {

    @Mock
    private LicenseService licenseService;

    @Mock
    private InstallationRepository installationRepository;

    @Mock
    private HttpServletRequest servletRequest;

    private PluginService pluginService;

    @BeforeEach
    void setUp() {
        pluginService = new PluginService(licenseService, installationRepository, new RequestRateLimiter());
    }

    @Test
    void validate_validLicense_recordsAndReturnsValid() {
        UUID instance = UUID.randomUUID();
        PluginValidateRequest req = new PluginValidateRequest("GOLIATH-KEY", instance.toString(), 25565, "1.0.0", "1.20.1");

        License lic = new License();
        lic.setId(UUID.randomUUID());
        lic.setServerIp("127.0.0.1");
        lic.setServerPort(25565);
        lic.setExpiration(LicenseExpiration.ONE_DAY);
        lic.setExpiresAt(Instant.now().plusSeconds(3600));
        lic.setStatus(LicenseStatus.ACTIVE);

        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(licenseService.findMatchingLicense("GOLIATH-KEY")).thenReturn(lic);
        when(installationRepository.findById(instance)).thenReturn(Optional.empty());
        when(installationRepository.save(any(Installation.class))).thenAnswer(i -> i.getArgument(0));

        var resp = pluginService.validate(req, servletRequest);

        assertThat(resp.code()).isEqualTo("VALID");
        assertThat(resp.licenseId()).isEqualTo(lic.getId());
        verify(installationRepository).save(any(Installation.class));
    }

    @Test
    void validate_missingKey_recordsUnlicensedAndReturnsInvalid() {
        UUID instance = UUID.randomUUID();
        PluginValidateRequest req = new PluginValidateRequest("", instance.toString(), 25565, "1.0.0", "1.20.1");

        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(installationRepository.findById(instance)).thenReturn(Optional.empty());
        when(installationRepository.save(any(Installation.class))).thenAnswer(i -> i.getArgument(0));

        var resp = pluginService.validate(req, servletRequest);

        assertThat(resp.code()).isEqualTo("INVALID");
        assertThat(resp.licenseId()).isNull();
        verify(installationRepository).save(any(Installation.class));
    }

    @Test
    void heartbeat_createsOrUpdatesInstallation() {
        UUID instance = UUID.randomUUID();
        PluginHeartbeatRequest req = new PluginHeartbeatRequest(instance.toString(), "1.0.0", "1.20.1");

        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(installationRepository.findById(instance)).thenReturn(Optional.empty());
        when(installationRepository.save(any(Installation.class))).thenAnswer(i -> i.getArgument(0));

        var resp = pluginService.heartbeat(req, servletRequest);
        assertThat(resp.status()).isEqualTo("OK");
        assertThat(resp.licenseStatus()).isEqualTo(InstallationStatus.UNLICENSED.name());
        verify(installationRepository).save(any(Installation.class));
    }
}
