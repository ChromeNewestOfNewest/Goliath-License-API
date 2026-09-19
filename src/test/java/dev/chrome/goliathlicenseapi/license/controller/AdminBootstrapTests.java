package dev.chrome.goliathlicenseapi.license.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.chrome.goliathlicenseapi.license.model.AdminUser;
import dev.chrome.goliathlicenseapi.license.repository.AdminUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import dev.chrome.goliathlicenseapi.license.service.AdminBootstrap;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTests {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void existingAdminPasswordIsSynchronized() {
        AdminUser existing = new AdminUser();
        existing.setUsername("owner");
        existing.setPasswordHash("old-hash");
        when(adminUserRepository.findByUsername("owner")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("new-secret")).thenReturn("encoded-new");

        AdminBootstrap bootstrap = new AdminBootstrap(adminUserRepository, passwordEncoder, "owner", "new-secret");
        bootstrap.initializeDefaultOwnerUser();

        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserRepository).save(captor.capture());
        AdminUser saved = captor.getValue();
        assertEquals("owner", saved.getUsername());
        assertEquals("encoded-new", saved.getPasswordHash());
        assertEquals(true, saved.isEnabled());
    }

    @Test
    void missingAdminIsCreated() {
        when(adminUserRepository.findByUsername("owner")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("encoded-pw");

        AdminBootstrap bootstrap = new AdminBootstrap(adminUserRepository, passwordEncoder, "owner", "pw");
        bootstrap.initializeDefaultOwnerUser();

        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserRepository).save(captor.capture());
        AdminUser saved = captor.getValue();
        assertEquals("owner", saved.getUsername());
        assertEquals("encoded-pw", saved.getPasswordHash());
        assertEquals(true, saved.isEnabled());
    }
}
