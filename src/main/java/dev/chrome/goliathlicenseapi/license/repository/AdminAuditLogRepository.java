package dev.chrome.goliathlicenseapi.license.repository;

import dev.chrome.goliathlicenseapi.license.model.AdminAuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {
}
