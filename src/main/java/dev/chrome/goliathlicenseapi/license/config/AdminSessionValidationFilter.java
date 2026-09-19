package dev.chrome.goliathlicenseapi.license.config;

import dev.chrome.goliathlicenseapi.license.model.AdminUser;
import dev.chrome.goliathlicenseapi.license.repository.AdminUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AdminSessionValidationFilter extends HttpFilter {

    private final AdminUserRepository adminUserRepository;

    public AdminSessionValidationFilter(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            AdminUser user = adminUserRepository.findByUsername(auth.getName()).orElse(null);
            if (user != null && user.getPasswordChangedAt() != null && request.getSession(false) != null) {
                try {
                    long sessionCreation = request.getSession(false).getCreationTime();
                    long changedAt = user.getPasswordChangedAt().toEpochMilli();
                    if (sessionCreation < changedAt) {
                        try { request.getSession(false).invalidate(); } catch (Exception ignore) {}
                        SecurityContextHolder.clearContext();
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"code\":\"SESSION_INVALIDATED\",\"message\":\"Session invalidated due to password change. Please log in again.\"}");
                        return;
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        chain.doFilter(request, response);
    }
}
