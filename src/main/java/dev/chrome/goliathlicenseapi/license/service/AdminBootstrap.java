package dev.chrome.goliathlicenseapi.license.service;

import dev.chrome.goliathlicenseapi.license.model.AdminUser;
import dev.chrome.goliathlicenseapi.license.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
public class AdminBootstrap {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminBootstrap(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${APP_ADMIN_USERNAME:owner}") String adminUsername,
            @Value("${APP_ADMIN_PASSWORD:chromeRegionsDevReload100}") String adminPassword) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDefaultOwnerUser() {
        try {
            AdminUser adminUser = adminUserRepository.findByUsername(adminUsername).orElseGet(() -> {
                AdminUser u = new AdminUser();
                u.setUsername(adminUsername);
                return u;
            });

            // Always update the password hash to match configured password and ensure enabled
            adminUser.setPasswordHash(passwordEncoder.encode(adminPassword));
            adminUser.setEnabled(true);
            adminUserRepository.save(adminUser);

            log.info("Admin account initialized: {}", adminUsername);
        } catch (Exception ex) {
            log.error("Failed to initialize admin account: {}", adminUsername, ex);
        }
    }
}
