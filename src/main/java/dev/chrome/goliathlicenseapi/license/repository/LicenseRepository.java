package dev.chrome.goliathlicenseapi.license.repository;

import dev.chrome.goliathlicenseapi.license.model.License;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseRepository extends JpaRepository<License, UUID> {
    List<License> findAllByOrderByCreatedAtDesc();
}
