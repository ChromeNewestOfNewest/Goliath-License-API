package dev.chrome.goliathlicenseapi.license.controller;

import dev.chrome.goliathlicenseapi.license.dto.AdminAnalyticsResponse;
import dev.chrome.goliathlicenseapi.license.dto.AdminLicenseExpirationRequest;
import dev.chrome.goliathlicenseapi.license.dto.AdminLicenseSummary;
import dev.chrome.goliathlicenseapi.license.dto.AdminOverviewResponse;
import dev.chrome.goliathlicenseapi.license.dto.AdminServerSummary;
import dev.chrome.goliathlicenseapi.license.dto.GenerateLicenseRequest;
import dev.chrome.goliathlicenseapi.license.dto.GenerateLicenseResponse;
import dev.chrome.goliathlicenseapi.license.dto.RevokeLicenseResponse;
import dev.chrome.goliathlicenseapi.license.dto.RemoteActionResponse;
import dev.chrome.goliathlicenseapi.license.model.AdminAuditLog;
import dev.chrome.goliathlicenseapi.license.model.License;
import dev.chrome.goliathlicenseapi.license.model.LicenseExpiration;
import dev.chrome.goliathlicenseapi.license.model.LicenseStatus;
import dev.chrome.goliathlicenseapi.license.model.RemoteAction;
import dev.chrome.goliathlicenseapi.license.model.RemoteActionType;
import dev.chrome.goliathlicenseapi.license.model.RemoteActionStatus;
import dev.chrome.goliathlicenseapi.license.repository.AdminAuditLogRepository;
import dev.chrome.goliathlicenseapi.license.repository.LicenseRepository;
import dev.chrome.goliathlicenseapi.license.service.AdminDashboardService;
import dev.chrome.goliathlicenseapi.license.service.LicenseService;
import dev.chrome.goliathlicenseapi.license.service.RemoteActionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import dev.chrome.goliathlicenseapi.license.service.RequestRateLimiter;
import dev.chrome.goliathlicenseapi.license.service.AdminAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final LicenseService licenseService;
    private final LicenseRepository licenseRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final RequestRateLimiter rateLimiter;
    private final AdminAuthService adminAuthService;
    private final RemoteActionService remoteActionService;

    // Backwards-compatible constructor (keeps existing tests and wiring working)
    public AdminController(
            AdminDashboardService adminDashboardService,
            LicenseService licenseService,
            LicenseRepository licenseRepository,
            AdminAuditLogRepository adminAuditLogRepository,
            RequestRateLimiter rateLimiter,
            AdminAuthService adminAuthService) {
        this(adminDashboardService, licenseService, licenseRepository, adminAuditLogRepository, rateLimiter, adminAuthService, null);
    }

    @Autowired
    public AdminController(
            AdminDashboardService adminDashboardService,
            LicenseService licenseService,
            LicenseRepository licenseRepository,
            AdminAuditLogRepository adminAuditLogRepository,
            RequestRateLimiter rateLimiter,
            AdminAuthService adminAuthService,
            RemoteActionService remoteActionService) {
        this.adminDashboardService = adminDashboardService;
        this.licenseService = licenseService;
        this.licenseRepository = licenseRepository;
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.rateLimiter = rateLimiter;
        this.adminAuthService = adminAuthService;
        this.remoteActionService = remoteActionService;
    }

    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewResponse> overview() {
        return ResponseEntity.ok(adminDashboardService.overview());
    }

    @GetMapping("/servers")
    public ResponseEntity<List<AdminServerSummary>> servers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String goliathVersion,
            @RequestParam(required = false) String minecraftVersion,
            @RequestParam(required = false, defaultValue = "false") Boolean onlineOnly,
            HttpServletRequest request) {
        logAction("LIST_SERVERS", "List/search/filter servers", request.getRemoteAddr());
        return ResponseEntity.ok(adminDashboardService.servers(search, status, goliathVersion, minecraftVersion, onlineOnly));
    }

    @GetMapping("/licenses")
    public ResponseEntity<List<AdminLicenseSummary>> licenses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String expiration,
            HttpServletRequest request) {
        logAction("LIST_LICENSES", "List/search licenses", request.getRemoteAddr());
        return ResponseEntity.ok(adminDashboardService.licenses(search, status, expiration));
    }

    @PostMapping("/licenses")
    public ResponseEntity<GenerateLicenseResponse> generateLicense(@Valid @RequestBody GenerateLicenseRequest request, HttpServletRequest servletRequest) {
        String rateKey = "admin:generate_license:" + servletRequest.getRemoteAddr();
        if (rateLimiter.isBlocked(rateKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        rateLimiter.record(rateKey);

        GenerateLicenseResponse response = licenseService.generateLicense(request);
        logAction("GENERATE_LICENSE", "Generated license for server " + request.serverIp() + ":" + request.serverPort(), servletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/licenses/{id}")
    public ResponseEntity<AdminLicenseSummary> findLicense(@PathVariable UUID id) {
        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("License not found."));
        return ResponseEntity.ok(mapLicense(license));
    }

    @PatchMapping("/account/password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String current = body.get("currentPassword");
        String next = body.get("newPassword");
        if (current == null || next == null || current.isBlank() || next.isBlank()) {
            throw new IllegalArgumentException("Both currentPassword and newPassword are required.");
        }
        adminAuthService.changePassword(current, next);
        logAction("PASSWORD_CHANGE", "Admin changed password.", request.getRemoteAddr());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "OK");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/servers/{instanceId}/block")
    public ResponseEntity<Map<String, Object>> blockServer(@PathVariable UUID instanceId, @RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        String reason = body == null ? "Blocked by admin." : body.getOrDefault("reason", "Blocked by admin.");
        adminDashboardService.blockInstallation(instanceId, reason);
        logAction("BLOCK_INSTALLATION", "Blocked installation " + instanceId + ": " + reason, request.getRemoteAddr());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "BLOCKED");
        resp.put("id", instanceId);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/servers/{instanceId}/unblock")
    public ResponseEntity<Map<String, Object>> unblockServer(@PathVariable UUID instanceId, HttpServletRequest request) {
        adminDashboardService.unblockInstallation(instanceId);
        logAction("UNBLOCK_INSTALLATION", "Unblocked installation " + instanceId, request.getRemoteAddr());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "UNBLOCKED");
        resp.put("id", instanceId);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/licenses/{id}/revoke")
    public ResponseEntity<RevokeLicenseResponse> revokeLicense(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("License not found."));
        String reason = body == null || body.get("reason") == null || body.get("reason").isBlank()
                ? "Revoked by admin console."
                : body.get("reason").trim();
        if (license.getStatus() == LicenseStatus.REVOKED) {
            return ResponseEntity.ok(new RevokeLicenseResponse(true, license.getId(), "License is already revoked.", LicenseStatus.REVOKED, license.getRevokedAt(), license.getRevokedReason()));
        }
        license.setStatus(LicenseStatus.REVOKED);
        license.setRevokedAt(Instant.now());
        license.setRevokedReason(reason);
        licenseRepository.save(license);
        logAction("REVOKE_LICENSE", "Revoked license " + id, request.getRemoteAddr());
        return ResponseEntity.ok(new RevokeLicenseResponse(true, license.getId(), "License revoked successfully.", LicenseStatus.REVOKED, license.getRevokedAt(), reason));
    }

    @DeleteMapping("/licenses/{id}")
    public ResponseEntity<Map<String, Object>> deleteLicense(@PathVariable UUID id, HttpServletRequest request) {
        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("License not found."));
        licenseRepository.delete(license);
        logAction("DELETE_LICENSE", "Deleted license " + id, request.getRemoteAddr());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "DELETED");
        body.put("id", id);
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/licenses/{id}/expiration")
    public ResponseEntity<AdminLicenseSummary> updateExpiration(
            @PathVariable UUID id,
            @Valid @RequestBody AdminLicenseExpirationRequest request,
            HttpServletRequest servletRequest) {
        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("License not found."));
        LicenseExpiration expiration = LicenseExpiration.fromValue(request.expiration());
        license.setExpiration(expiration);
        license.setExpiresAt(expiration == LicenseExpiration.NEVER ? null : Instant.now().plus(expiration.getDuration()));
        licenseRepository.save(license);
        logAction("UPDATE_LICENSE_EXPIRATION", "Updated expiration on license " + id + " to " + expiration.name(), servletRequest.getRemoteAddr());
        return ResponseEntity.ok(mapLicense(license));
    }

    @GetMapping("/analytics")
    public ResponseEntity<AdminAnalyticsResponse> analytics() {
        return ResponseEntity.ok(adminDashboardService.analytics());
    }

    @PostMapping("/servers/{instanceId}/actions/restart")
    public ResponseEntity<Map<String,Object>> restartServer(@PathVariable UUID instanceId, HttpServletRequest request) {
        String rateKey = "admin:action:restart:" + request.getRemoteAddr();
        if (rateLimiter.isBlocked(rateKey)) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        rateLimiter.record(rateKey);
        String admin = SecurityContextHolder.getContext().getAuthentication() == null ? "unknown" : SecurityContextHolder.getContext().getAuthentication().getName();
        RemoteAction a = remoteActionService.createAction(null, instanceId, RemoteActionType.RESTART_SERVER, admin, Instant.now());
        logAction("REMOTE_ACTION_CREATE", "Created restart action " + a.getActionId() + " for " + instanceId, request.getRemoteAddr());
        Map<String,Object> resp = new LinkedHashMap<>();
        resp.put("status","PENDING");
        resp.put("actionId", a.getActionId());
        resp.put("expiresAt", a.getExpiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/servers/{instanceId}/actions/stop")
    public ResponseEntity<Map<String,Object>> stopServer(@PathVariable UUID instanceId, HttpServletRequest request) {
        String rateKey = "admin:action:stop:" + request.getRemoteAddr();
        if (rateLimiter.isBlocked(rateKey)) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        rateLimiter.record(rateKey);
        String admin = SecurityContextHolder.getContext().getAuthentication() == null ? "unknown" : SecurityContextHolder.getContext().getAuthentication().getName();
        RemoteAction a = remoteActionService.createAction(null, instanceId, RemoteActionType.STOP_SERVER, admin, Instant.now());
        logAction("REMOTE_ACTION_CREATE", "Created stop action " + a.getActionId() + " for " + instanceId, request.getRemoteAddr());
        Map<String,Object> resp = new LinkedHashMap<>();
        resp.put("status","PENDING");
        resp.put("actionId", a.getActionId());
        resp.put("expiresAt", a.getExpiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/servers/{instanceId}/actions/remove-goliath")
    public ResponseEntity<Map<String,Object>> removeGoliath(@PathVariable UUID instanceId, HttpServletRequest request) {
        String rateKey = "admin:action:remove:" + request.getRemoteAddr();
        if (rateLimiter.isBlocked(rateKey)) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        rateLimiter.record(rateKey);
        String admin = SecurityContextHolder.getContext().getAuthentication() == null ? "unknown" : SecurityContextHolder.getContext().getAuthentication().getName();
        RemoteAction a = remoteActionService.createAction(null, instanceId, RemoteActionType.REMOVE_GOLIATH, admin, Instant.now());
        logAction("REMOTE_ACTION_CREATE", "Created remove-goliath action " + a.getActionId() + " for " + instanceId, request.getRemoteAddr());
        Map<String,Object> resp = new LinkedHashMap<>();
        resp.put("status","PENDING");
        resp.put("actionId", a.getActionId());
        resp.put("expiresAt", a.getExpiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/servers/{instanceId}/actions")
    public ResponseEntity<List<RemoteActionResponse>> recentActions(@PathVariable UUID instanceId) {
        List<RemoteAction> actions = remoteActionService.recentForInstance(instanceId);
        List<RemoteActionResponse> resp = actions.stream().map(a -> new RemoteActionResponse(
                a.getActionId(), a.getTargetInstanceId(), a.getActionType(), a.getStatus(), a.getRequestedAt(), a.getReceivedAt(), a.getCompletedAt(), a.getExpiresAt(), a.getSafeFailureReason()
        )).toList();
        return ResponseEntity.ok(resp);
    }

    private AdminLicenseSummary mapLicense(License license) {
        return new AdminLicenseSummary(
                license.getId(),
                license.getServerIp(),
                license.getServerPort(),
                license.getExpiration(),
                license.getExpiration() == null ? null : license.getExpiration().getValue(),
                license.getCreatedAt(),
                license.getExpiresAt(),
                license.getStatus(),
                license.getRevokedAt(),
                license.getRevokedReason()
        );
    }

    private void logAction(String action, String details, String ipAddress) {
        AdminAuditLog log = new AdminAuditLog();
        log.setId(UUID.randomUUID());
        log.setAdminUsername(SecurityContextHolder.getContext().getAuthentication() == null
                ? "system"
                : SecurityContextHolder.getContext().getAuthentication().getName());
        log.setAction(action);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(Instant.now());
        adminAuditLogRepository.save(log);
    }
}
