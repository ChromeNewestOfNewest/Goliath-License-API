package dev.chrome.goliathlicenseapi.license.repository;

import dev.chrome.goliathlicenseapi.license.model.RemoteAction;
import dev.chrome.goliathlicenseapi.license.model.RemoteActionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RemoteActionRepository extends JpaRepository<RemoteAction, UUID> {
    List<RemoteAction> findTop50ByTargetInstanceIdOrderByRequestedAtDesc(UUID instanceId);
    List<RemoteAction> findByTargetInstanceIdAndStatus(UUID instanceId, RemoteActionStatus status);

    @Modifying
    @Query("UPDATE RemoteAction r SET r.status = :newStatus, r.receivedAt = :receivedAt WHERE r.actionId = :actionId AND r.status = :expectedStatus")
    int claimAction(@Param("actionId") UUID actionId, @Param("expectedStatus") RemoteActionStatus expectedStatus, @Param("newStatus") RemoteActionStatus newStatus, @Param("receivedAt") java.time.Instant receivedAt);

}
