package dev.chrome.goliathlicenseapi.license.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import dev.chrome.goliathlicenseapi.license.dto.AdminLicenseSummary;
import dev.chrome.goliathlicenseapi.license.dto.AdminLoginRequest;
import dev.chrome.goliathlicenseapi.license.dto.AdminLoginResponse;
import dev.chrome.goliathlicenseapi.license.dto.AdminMeResponse;
import dev.chrome.goliathlicenseapi.license.dto.GenerateLicenseRequest;
import dev.chrome.goliathlicenseapi.license.dto.GenerateLicenseResponse;
import dev.chrome.goliathlicenseapi.license.model.AdminUser;
import dev.chrome.goliathlicenseapi.license.model.License;
import dev.chrome.goliathlicenseapi.license.model.LicenseExpiration;
import dev.chrome.goliathlicenseapi.license.model.LicenseStatus;
import dev.chrome.goliathlicenseapi.license.repository.AdminAuditLogRepository;
import dev.chrome.goliathlicenseapi.license.repository.AdminUserRepository;
import dev.chrome.goliathlicenseapi.license.repository.InstallationRepository;
import dev.chrome.goliathlicenseapi.license.repository.LicenseRepository;
import dev.chrome.goliathlicenseapi.license.service.AdminAuthService;
import dev.chrome.goliathlicenseapi.license.service.AdminDashboardService;
import dev.chrome.goliathlicenseapi.license.service.LicenseService;
import dev.chrome.goliathlicenseapi.license.service.LoginRateLimiter;
import dev.chrome.goliathlicenseapi.license.service.RequestRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminAuthIntegrationTests {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    @Mock
    private InstallationRepository installationRepository;

    @Mock
    private LicenseRepository licenseRepository;

    @Mock
    private HttpServletRequest servletRequest;

    @Test
    void adminLoginFailsForBadCredentials() {
        AdminAuthService service = new AdminAuthService(authenticationManager, adminUserRepository, adminAuditLogRepository, new LoginRateLimiter(), new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));

        ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
                () -> service.login(new AdminLoginRequest("owner", "wrong"), servletRequest));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminUserCanBeLoadedAndMeReturnsDetails() {
        AdminUser admin = new AdminUser();
        admin.setUsername("owner");
        admin.setLastLoginAt(Instant.now());
        when(adminUserRepository.findByUsername("owner")).thenReturn(Optional.of(admin));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", "pw", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        AdminAuthService service = new AdminAuthService(authenticationManager, adminUserRepository, adminAuditLogRepository, new LoginRateLimiter(), new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());

        AdminMeResponse me = service.me();
        assertThat(me.username()).isEqualTo("owner");
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminGenerationReturnsPlaintextOnceAndListingDoesNotExposeIt() {
        when(licenseRepository.save(any(License.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LicenseService service = new LicenseService(licenseRepository);
        GenerateLicenseResponse generated = service.generateLicense(new GenerateLicenseRequest("127.0.0.1", 25565, "1d"));
        assertThat(generated.licenseKey()).startsWith("GOLIATH-");

        License stored = new License();
        stored.setId(UUID.randomUUID());
        stored.setServerIp("127.0.0.1");
        stored.setServerPort(25565);
        stored.setExpiration(LicenseExpiration.ONE_DAY);
        stored.setCreatedAt(Instant.now());
        stored.setStatus(LicenseStatus.ACTIVE);
        stored.setExpiresAt(Instant.now().plusSeconds(86400));
        stored.setLicenseKeyHash("hash");
        stored.setLicenseKeySalt("salt");

        when(licenseRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(stored));
        when(licenseRepository.findById(stored.getId())).thenReturn(Optional.of(stored));

        AdminAuthService authService = new AdminAuthService(authenticationManager, adminUserRepository, adminAuditLogRepository, new LoginRateLimiter(), new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
        AdminController controller = new AdminController(new AdminDashboardService(installationRepository, licenseRepository), service, licenseRepository, adminAuditLogRepository, new RequestRateLimiter(), authService);

        var list = controller.licenses(null, null, null, servletRequest);
        assertThat(list.getBody()).isNotNull();
        assertThat(list.getBody().getFirst().expirationValue()).isEqualTo("1d");

        var detail = controller.findLicense(stored.getId());
        assertThat(detail.getBody()).isNotNull();
        assertThat(detail.getBody().expirationValue()).isEqualTo("1d");
    }

    @Test
    void dashboardOverviewAndAnalyticsAggregateServerAndLicenseStats() {
        AdminDashboardService service = new AdminDashboardService(installationRepository, licenseRepository);
        when(installationRepository.findAllByOrderByLastSeenDesc()).thenReturn(List.of());
        when(licenseRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        var overview = service.overview();
        assertThat(overview.totalServers()).isZero();
        assertThat(overview.onlineServers()).isZero();

        var analytics = service.analytics();
        assertThat(analytics.totalLicenses()).isZero();
        assertThat(analytics.totalInstallations()).isZero();
    }
}
