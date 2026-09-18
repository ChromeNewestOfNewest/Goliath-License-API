package dev.chrome.goliathlicenseapi.license.service;

import dev.chrome.goliathlicenseapi.license.dto.AdminLoginRequest;
import dev.chrome.goliathlicenseapi.license.dto.AdminLoginResponse;
import dev.chrome.goliathlicenseapi.license.dto.AdminMeResponse;
import dev.chrome.goliathlicenseapi.license.model.AdminAuditLog;
import dev.chrome.goliathlicenseapi.license.model.AdminUser;
import dev.chrome.goliathlicenseapi.license.repository.AdminAuditLogRepository;
import dev.chrome.goliathlicenseapi.license.repository.AdminUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AdminAuthService {

    private final AuthenticationManager authenticationManager;
    private final AdminUserRepository adminUserRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final LoginRateLimiter loginRateLimiter;

    public AdminAuthService(
            AuthenticationManager authenticationManager,
            AdminUserRepository adminUserRepository,
            AdminAuditLogRepository adminAuditLogRepository,
            LoginRateLimiter loginRateLimiter) {
        this.authenticationManager = authenticationManager;
        this.adminUserRepository = adminUserRepository;
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.loginRateLimiter = loginRateLimiter;
    }

    public AdminLoginResponse login(AdminLoginRequest request, HttpServletRequest servletRequest) {
        String clientKey = resolveClientKey(servletRequest);
        if (loginRateLimiter.isBlocked(clientKey)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed login attempts. Please try again later.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            servletRequest.getSession(true).setAttribute("SPRING_SECURITY_CONTEXT", context);

            adminUserRepository.findByUsername(request.username().trim()).ifPresent(user -> {
                user.setLastLoginAt(Instant.now());
                adminUserRepository.save(user);
            });

            loginRateLimiter.recordSuccess(clientKey);
            recordAudit(request.username().trim(), "LOGIN", "Successful admin login.", servletRequest.getRemoteAddr());
            return new AdminLoginResponse("OK", request.username().trim(), "Authenticated successfully.");
        } catch (AuthenticationException ex) {
            loginRateLimiter.recordFailure(clientKey);
            recordAudit(request.username() == null ? "unknown" : request.username().trim(), "LOGIN_FAILED", "Failed admin login.", servletRequest.getRemoteAddr());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin credentials.");
        }
    }

    public void logout(HttpServletRequest servletRequest) {
        String username = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElse("unknown");
        SecurityContextHolder.clearContext();
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        recordAudit(username, "LOGOUT", "Admin logout.", servletRequest.getRemoteAddr());
    }

    public AdminMeResponse me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new IllegalStateException("No authenticated admin session.");
        }
        AdminUser user = adminUserRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Admin user not found."));
        return new AdminMeResponse(user.getUsername(), "ADMIN", user.getLastLoginAt());
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private void recordAudit(String username, String action, String details, String ip) {
        AdminAuditLog log = new AdminAuditLog();
        log.setId(UUID.randomUUID());
        log.setAdminUsername(username == null ? "unknown" : username);
        log.setAction(action);
        log.setDetails(details);
        log.setIpAddress(ip);
        log.setCreatedAt(Instant.now());
        adminAuditLogRepository.save(log);
    }
}
