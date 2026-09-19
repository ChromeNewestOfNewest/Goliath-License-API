package dev.chrome.goliathlicenseapi.license.service;

import dev.chrome.goliathlicenseapi.license.model.RemoteAction;
import dev.chrome.goliathlicenseapi.license.model.RemoteActionStatus;
import dev.chrome.goliathlicenseapi.license.model.RemoteActionType;
import dev.chrome.goliathlicenseapi.license.model.Installation;
import dev.chrome.goliathlicenseapi.license.model.License;
import dev.chrome.goliathlicenseapi.license.repository.RemoteActionRepository;
import dev.chrome.goliathlicenseapi.license.repository.InstallationRepository;
import dev.chrome.goliathlicenseapi.license.service.LicenseService;
import dev.chrome.goliathlicenseapi.license.dto.RemoteActionResponse;
import dev.chrome.goliathlicenseapi.license.dto.PluginActionPollResponse;
import dev.chrome.goliathlicenseapi.license.dto.PluginActionClaimRequest;
import dev.chrome.goliathlicenseapi.license.dto.PluginActionReportRequest;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@Transactional
public class RemoteActionService {

    private final RemoteActionRepository remoteActionRepository;
    private final InstallationRepository installationRepository;
    private final LicenseService licenseService;

    // actions expire after this duration
    private final Duration defaultExpiry = Duration.ofMinutes(10);

    public RemoteActionService(RemoteActionRepository remoteActionRepository, InstallationRepository installationRepository, LicenseService licenseService) {
        this.remoteActionRepository = remoteActionRepository;
        this.installationRepository = installationRepository;
        this.licenseService = licenseService;
    }

    public RemoteAction createAction(UUID adminRequestedByInstallationNotUsed, UUID targetInstanceId, RemoteActionType type, String requestedBy, Instant now) {
        // Prevent duplicate pending actions of same type
        List<RemoteAction> pendings = remoteActionRepository.findByTargetInstanceIdAndStatus(targetInstanceId, RemoteActionStatus.PENDING);
        for (RemoteAction r : pendings) {
            if (r.getActionType() == type) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending action of this type already exists for the target.");
            }
        }

        RemoteAction r = new RemoteAction();
        r.setActionId(UUID.randomUUID());
        r.setTargetInstanceId(targetInstanceId);
        r.setActionType(type);
        r.setStatus(RemoteActionStatus.PENDING);
        r.setRequestedAt(now);
        r.setRequestedBy(requestedBy == null ? "unknown" : requestedBy);
        r.setExpiresAt(now.plus(defaultExpiry));
        remoteActionRepository.save(r);
        return r;
    }

    public List<RemoteAction> recentForInstance(UUID instanceId) {
        return remoteActionRepository.findTop50ByTargetInstanceIdOrderByRequestedAtDesc(instanceId);
    }

    public PluginActionPollResponse pollForInstance(String instanceIdStr, String licenseKey, String observedIp) {
        UUID instanceId;
        try { instanceId = UUID.fromString(instanceIdStr); } catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instanceId must be a valid UUID"); }

        // Authenticate plugin: ensure licenseKey maps to the same license and installation
        License license;
        try { license = licenseService.findMatchingLicense(licenseKey); } catch (Exception ex) { license = null; }
        if (license == null) return new PluginActionPollResponse(false, null, null, null, null);
        Optional<Installation> maybe = installationRepository.findById(instanceId);
        if (maybe.isEmpty()) return new PluginActionPollResponse(false, null, null, null, null);
        Installation inst = maybe.get();
        if (inst.getAssociatedLicenseId() == null || !inst.getAssociatedLicenseId().equals(license.getId())) return new PluginActionPollResponse(false, null, null, null, null);

        // find pending action
        List<RemoteAction> pendings = remoteActionRepository.findByTargetInstanceIdAndStatus(instanceId, RemoteActionStatus.PENDING);
        if (pendings.isEmpty()) return new PluginActionPollResponse(false, null, null, null, null);
        RemoteAction first = pendings.get(0);
        // if expired mark expired
        Instant now = Instant.now();
        if (first.getExpiresAt() != null && first.getExpiresAt().isBefore(now)) {
            first.setStatus(RemoteActionStatus.EXPIRED);
            remoteActionRepository.save(first);
            return new PluginActionPollResponse(false, null, null, null, null);
        }
        return new PluginActionPollResponse(true, first.getActionId(), first.getActionType(), first.getRequestedAt(), first.getExpiresAt());
    }

    public boolean claimAction(String instanceIdStr, PluginActionClaimRequest req, String observedIp) {
        UUID instanceId;
        try { instanceId = UUID.fromString(req.instanceId()); } catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instanceId must be a valid UUID"); }

        License license;
        try { license = licenseService.findMatchingLicense(req.licenseKey()); } catch (Exception ex) { license = null; }
        if (license == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid license");

        Optional<Installation> maybe = installationRepository.findById(instanceId);
        if (maybe.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Installation not found");
        Installation inst = maybe.get();
        if (inst.getAssociatedLicenseId() == null || !inst.getAssociatedLicenseId().equals(license.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-instance access denied");

        // find a pending action and atomically claim
        List<RemoteAction> pendings = remoteActionRepository.findByTargetInstanceIdAndStatus(instanceId, RemoteActionStatus.PENDING);
        if (pendings.isEmpty()) return false;
        RemoteAction action = pendings.get(0);
        int updated = remoteActionRepository.claimAction(action.getActionId(), RemoteActionStatus.PENDING, RemoteActionStatus.RECEIVED, Instant.now());
        return updated > 0;
    }

    public void reportResult(PluginActionReportRequest req, String observedIp) {
        UUID actionId = req.actionId();
        Optional<RemoteAction> maybe = remoteActionRepository.findById(actionId);
        if (maybe.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found");
        RemoteAction action = maybe.get();

        // validate instance and license to prevent cross-instance
        UUID instanceId;
        try { instanceId = UUID.fromString(req.instanceId()); } catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instanceId must be a valid UUID"); }
        License license;
        try { license = licenseService.findMatchingLicense(req.licenseKey()); } catch (Exception ex) { license = null; }
        if (license == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid license");
        Optional<Installation> maybeInst = installationRepository.findById(instanceId);
        if (maybeInst.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Installation not found");
        Installation inst = maybeInst.get();
        if (inst.getAssociatedLicenseId() == null || !inst.getAssociatedLicenseId().equals(license.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-instance access denied");

        // ensure action belongs to instance
        if (!action.getTargetInstanceId().equals(instanceId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action does not belong to this instance");

        if (action.getStatus() != RemoteActionStatus.RECEIVED && action.getStatus() != RemoteActionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Action is not in a receivable state");
        }

        if ("COMPLETED".equalsIgnoreCase(req.result())) {
            action.setStatus(RemoteActionStatus.COMPLETED);
            action.setCompletedAt(Instant.now());
            action.setSafeFailureReason(null);
        } else {
            action.setStatus(RemoteActionStatus.FAILED);
            action.setCompletedAt(Instant.now());
            action.setSafeFailureReason(req.failureReason());
        }
        remoteActionRepository.save(action);
    }
}
