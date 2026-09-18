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

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminBootstrap(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:owner}") String adminUsername,
            @Value("${app.admin.password:change-me-please}") String adminPassword) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDefaultOwnerUser() {
        if (!adminUserRepository.existsByUsername(adminUsername)) {
            AdminUser adminUser = new AdminUser();
            adminUser.setUsername(adminUsername);
            adminUser.setPasswordHash(passwordEncoder.encode(adminPassword));
            adminUser.setEnabled(true);
            adminUserRepository.save(adminUser);
        }
    }
}
