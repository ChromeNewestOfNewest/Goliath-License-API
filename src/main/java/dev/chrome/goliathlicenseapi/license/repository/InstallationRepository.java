package dev.chrome.goliathlicenseapi.license.repository;

import dev.chrome.goliathlicenseapi.license.model.Installation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallationRepository extends JpaRepository<Installation, UUID> {
}
