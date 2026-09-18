package dev.chrome.goliathlicenseapi.license.repository;

import dev.chrome.goliathlicenseapi.license.model.AdminUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    Optional<AdminUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
