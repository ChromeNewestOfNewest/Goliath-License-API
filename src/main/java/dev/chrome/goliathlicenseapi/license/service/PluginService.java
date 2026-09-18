package dev.chrome.goliathlicenseapi.license.service;

import dev.chrome.goliathlicenseapi.license.dto.PluginHeartbeatRequest;
import dev.chrome.goliathlicenseapi.license.dto.PluginHeartbeatResponse;
import dev.chrome.goliathlicenseapi.license.dto.PluginValidateRequest;
import dev.chrome.goliathlicenseapi.license.dto.PluginValidateResponse;
import dev.chrome.goliathlicenseapi.license.model.Installation;
import dev.chrome.goliathlicenseapi.license.model.InstallationStatus;
import dev.chrome.goliathlicenseapi.license.model.License;
import dev.chrome.goliathlicenseapi.license.model.LicenseExpiration;
import dev.chrome.goliathlicenseapi.license.model.LicenseStatus;
import dev.chrome.goliathlicenseapi.license.repository.InstallationRepository;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Service
@Transactional
public class PluginService {

    private final LicenseService licenseService;
    private final InstallationRepository installationRepository;
    private final Clock clock;

    public PluginService(LicenseService licenseService, InstallationRepository installationRepository) {
        this.licenseService = licenseService;
        this.installationRepository = installationRepository;
        this.clock = Clock.systemUTC();
    }

    private String extractRemoteIp(HttpServletRequest request) {
        if (request == null) return "";
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizeIp(String rawIp) {
        if (rawIp == null) return null;
        String candidate = rawIp.trim();
        if (candidate.contains(":")) {
            try {
                return InetAddress.getByName(candidate).getHostAddress();
            } catch (UnknownHostException ex) {
                return candidate;
            }
        }
        return candidate;
    }

    public PluginValidateResponse validate(PluginValidateRequest request, HttpServletRequest servletRequest) {
        Instant now = Instant.now(clock);
        String observedIp = normalizeIp(extractRemoteIp(servletRequest));
        UUID instanceUuid;
        try {
            instanceUuid = UUID.fromString(request.instanceId());
        } catch (Exception ex) {
            throw new IllegalArgumentException("instanceId must be a valid UUID");
        }

        InstallationStatus recordStatus = InstallationStatus.UNLICENSED;
        UUID associatedLicenseId = null;
        Instant expiresAt = null;
        String code = "INVALID";
        String message = "License is invalid.";

        // missing/blank license -> UNLICENSED
        if (request.licenseKey() == null || request.licenseKey().isBlank()) {
            recordStatus = InstallationStatus.UNLICENSED;
            code = "INVALID";
            message = "No license provided.";
        } else {
            License license;
            try {
                license = licenseService.findMatchingLicense(request.licenseKey());
            } catch (IllegalArgumentException ex) {
                // invalid license format
                license = null;
            }

            if (license == null) {
                recordStatus = InstallationStatus.INVALID;
                code = "INVALID";
                message = "License key is invalid.";
            } else {
                associatedLicenseId = license.getId();
                // revoked
                if (license.getStatus() == LicenseStatus.REVOKED) {
                    recordStatus = InstallationStatus.REVOKED;
                    code = "REVOKED";
                    message = "License revoked.";
                } else if (!license.getServerIp().equals(observedIp)) {
                    recordStatus = InstallationStatus.SERVER_MISMATCH;
                    code = "SERVER_MISMATCH";
                    message = "Server IP does not match license.";
                } else if (!license.getServerPort().equals(request.serverPort())) {
                    recordStatus = InstallationStatus.SERVER_MISMATCH;
                    code = "SERVER_MISMATCH";
                    message = "Server port does not match license.";
                } else if (license.isExpired(now)) {
                    recordStatus = InstallationStatus.EXPIRED;
                    code = "EXPIRED";
                    message = "License has expired.";
                } else {
                    recordStatus = InstallationStatus.VALID;
                    code = "VALID";
                    message = "License is valid.";
                    expiresAt = license.getExpiresAt();
                }
            }
        }

        // upsert installation
        Optional<Installation> maybe = installationRepository.findById(instanceUuid);
        Installation inst = maybe.orElseGet(() -> {
            Installation n = new Installation();
            n.setInstanceId(instanceUuid);
            n.setFirstSeen(now);
            return n;
        });

        inst.setObservedIp(observedIp != null ? observedIp : "");
        inst.setServerPort(request.serverPort());
        inst.setGoliathVersion(request.goliathVersion());
        inst.setMinecraftVersion(request.minecraftVersion());
        inst.setLastSeen(now);
        inst.setLicenseStatus(recordStatus);
        inst.setAssociatedLicenseId(associatedLicenseId);
        if ("VALID".equals(code)) {
            inst.setLastSuccessfulValidation(now);
        }
        installationRepository.save(inst);

        return new PluginValidateResponse(code, message, associatedLicenseId, expiresAt);
    }

    public PluginHeartbeatResponse heartbeat(PluginHeartbeatRequest request, HttpServletRequest servletRequest) {
        Instant now = Instant.now(clock);
        UUID instanceUuid;
        try {
            instanceUuid = UUID.fromString(request.instanceId());
        } catch (Exception ex) {
            throw new IllegalArgumentException("instanceId must be a valid UUID");
        }

        String observedIp = normalizeIp(extractRemoteIp(servletRequest));

        Optional<Installation> maybe = installationRepository.findById(instanceUuid);
        Installation inst = maybe.orElseGet(() -> {
            Installation n = new Installation();
            n.setInstanceId(instanceUuid);
            n.setFirstSeen(now);
            n.setObservedIp(observedIp != null ? observedIp : "");
            n.setServerPort(0);
            n.setGoliathVersion(request.goliathVersion());
            n.setMinecraftVersion(request.minecraftVersion());
            n.setLicenseStatus(InstallationStatus.UNLICENSED);
            return n;
        });

        inst.setLastSeen(now);
        inst.setObservedIp(observedIp != null ? observedIp : inst.getObservedIp());
        inst.setGoliathVersion(request.goliathVersion());
        inst.setMinecraftVersion(request.minecraftVersion());
        installationRepository.save(inst);

        return new PluginHeartbeatResponse("OK", "Heartbeat recorded.", inst.getInstanceId(), inst.getFirstSeen(), inst.getLastSeen(), inst.getLicenseStatus().name());
    }
}
