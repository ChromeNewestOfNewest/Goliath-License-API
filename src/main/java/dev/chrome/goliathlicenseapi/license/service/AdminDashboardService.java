package dev.chrome.goliathlicenseapi.license.service;

import dev.chrome.goliathlicenseapi.license.dto.AdminAnalyticsResponse;
import dev.chrome.goliathlicenseapi.license.dto.AdminLicenseSummary;
import dev.chrome.goliathlicenseapi.license.dto.AdminOverviewResponse;
import dev.chrome.goliathlicenseapi.license.dto.AdminServerSummary;
import dev.chrome.goliathlicenseapi.license.model.Installation;
import dev.chrome.goliathlicenseapi.license.model.InstallationStatus;
import dev.chrome.goliathlicenseapi.license.model.License;
import dev.chrome.goliathlicenseapi.license.model.LicenseExpiration;
import dev.chrome.goliathlicenseapi.license.model.LicenseStatus;
import dev.chrome.goliathlicenseapi.license.repository.InstallationRepository;
import dev.chrome.goliathlicenseapi.license.repository.LicenseRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final InstallationRepository installationRepository;
    private final LicenseRepository licenseRepository;

    public AdminDashboardService(InstallationRepository installationRepository, LicenseRepository licenseRepository) {
        this.installationRepository = installationRepository;
        this.licenseRepository = licenseRepository;
    }

    public AdminOverviewResponse overview() {
        List<Installation> installations = installationRepository.findAllByOrderByLastSeenDesc();
        List<License> licenses = licenseRepository.findAllByOrderByCreatedAtDesc();
        Instant now = Instant.now();

        long totalServers = installations.size();
        long onlineServers = installations.stream()
                .filter(i -> i.getLastSeen() != null && Duration.between(i.getLastSeen(), now).toMinutes() <= 5)
                .count();
        long activeLicenses = licenses.stream().filter(l -> l.getStatus() == LicenseStatus.ACTIVE).count();
        long expiredLicenses = licenses.stream().filter(l -> l.getStatus() == LicenseStatus.ACTIVE && l.isExpired(now)).count();
        long unlicensedServers = installations.stream()
                .filter(i -> i.getLicenseStatus() == null || i.getLicenseStatus() == InstallationStatus.UNLICENSED)
                .count();
        long revokedLicenses = licenses.stream().filter(l -> l.getStatus() == LicenseStatus.REVOKED).count();

        return new AdminOverviewResponse(
                totalServers,
                onlineServers,
                activeLicenses,
                expiredLicenses,
                unlicensedServers,
                revokedLicenses,
                totalServers,
                summarizeInstallations(installations, Installation::getGoliathVersion),
                summarizeInstallations(installations, Installation::getMinecraftVersion),
                summarizeLicenses(licenses, License::getExpiration)
        );
    }

    public List<AdminServerSummary> servers(String search, String status, String goliathVersion, String minecraftVersion, Boolean onlineOnly) {
        List<Installation> installations = installationRepository.findAllByOrderByLastSeenDesc();
        Instant now = Instant.now();
        List<AdminServerSummary> result = new ArrayList<>();
        for (Installation installation : installations) {
            if (search != null && !search.isBlank()) {
                String haystack = (installation.getObservedIp() + " " + installation.getGoliathVersion() + " " + installation.getMinecraftVersion() + " " + installation.getInstanceId()).toLowerCase();
                if (!haystack.contains(search.toLowerCase())) {
                    continue;
                }
            }
            if (status != null && !status.isBlank() && installation.getLicenseStatus() != null && !installation.getLicenseStatus().name().equalsIgnoreCase(status)) {
                continue;
            }
            if (goliathVersion != null && !goliathVersion.isBlank() && (installation.getGoliathVersion() == null || !installation.getGoliathVersion().equalsIgnoreCase(goliathVersion))) {
                continue;
            }
            if (minecraftVersion != null && !minecraftVersion.isBlank() && (installation.getMinecraftVersion() == null || !installation.getMinecraftVersion().equalsIgnoreCase(minecraftVersion))) {
                continue;
            }
            if (Boolean.TRUE.equals(onlineOnly)) {
                boolean online = installation.getLastSeen() != null && Duration.between(installation.getLastSeen(), now).toMinutes() <= 5;
                if (!online) {
                    continue;
                }
            }
            boolean online = installation.getLastSeen() != null && Duration.between(installation.getLastSeen(), now).toMinutes() <= 5;
            result.add(new AdminServerSummary(
                    installation.getInstanceId(),
                    installation.getObservedIp(),
                    installation.getServerPort(),
                    installation.getGoliathVersion(),
                    installation.getMinecraftVersion(),
                    installation.getFirstSeen(),
                    installation.getLastSeen(),
                    installation.getLicenseStatus(),
                    installation.getAssociatedLicenseId(),
                    online
            ));
        }
        return result;
    }

    public List<AdminLicenseSummary> licenses(String search, String status, String expiration) {
        List<License> licenses = licenseRepository.findAllByOrderByCreatedAtDesc();
        List<AdminLicenseSummary> result = new ArrayList<>();
        for (License license : licenses) {
            if (search != null && !search.isBlank()) {
                String haystack = (license.getServerIp() + " " + license.getExpiration() + " " + license.getStatus()).toLowerCase();
                if (!haystack.contains(search.toLowerCase())) {
                    continue;
                }
            }
            if (status != null && !status.isBlank() && license.getStatus() != null && !license.getStatus().name().equalsIgnoreCase(status)) {
                continue;
            }
            if (expiration != null && !expiration.isBlank() && (license.getExpiration() == null || !license.getExpiration().getValue().equalsIgnoreCase(expiration))) {
                continue;
            }
            result.add(new AdminLicenseSummary(
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
            ));
        }
        return result;
    }

    public AdminAnalyticsResponse analytics() {
        List<License> licenses = licenseRepository.findAllByOrderByCreatedAtDesc();
        List<Installation> installations = installationRepository.findAllByOrderByLastSeenDesc();
        Instant now = Instant.now();

        long totalLicenses = licenses.size();
        long activeLicenses = licenses.stream().filter(l -> l.getStatus() == LicenseStatus.ACTIVE).count();
        long expiredLicenses = licenses.stream().filter(l -> l.getStatus() == LicenseStatus.ACTIVE && l.isExpired(now)).count();
        long revokedLicenses = licenses.stream().filter(l -> l.getStatus() == LicenseStatus.REVOKED).count();
        long totalInstallations = installations.size();
        long onlineInstallations = installations.stream()
                .filter(i -> i.getLastSeen() != null && Duration.between(i.getLastSeen(), now).toMinutes() <= 5)
                .count();

        Map<String, Long> licenseCreationByDay = new LinkedHashMap<>();
        for (License license : licenses) {
            String day = license.getCreatedAt() == null ? "unknown" : license.getCreatedAt().toString().substring(0, 10);
            licenseCreationByDay.put(day, licenseCreationByDay.getOrDefault(day, 0L) + 1L);
        }

        Map<String, Long> versionDist = summarizeInstallations(installations, Installation::getGoliathVersion);
        Map<String, Long> minecraftDist = summarizeInstallations(installations, Installation::getMinecraftVersion);
        Map<String, Long> expirationDist = summarizeLicenses(licenses, License::getExpiration);

        return new AdminAnalyticsResponse(
                licenseCreationByDay,
                expirationDist,
                versionDist,
                minecraftDist,
                totalLicenses,
                activeLicenses,
                expiredLicenses,
                revokedLicenses,
                totalInstallations,
                onlineInstallations
        );
    }

    private Map<String, Long> summarizeInstallations(List<Installation> installations, java.util.function.Function<Installation, String> mapper) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Installation installation : installations) {
            String value = mapper.apply(installation);
            if (value == null || value.isBlank()) {
                value = "unknown";
            }
            counts.put(value, counts.getOrDefault(value, 0L) + 1L);
        }
        return counts;
    }

    private Map<String, Long> summarizeLicenses(List<License> licenses, java.util.function.Function<License, LicenseExpiration> mapper) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (License license : licenses) {
            LicenseExpiration expiration = mapper.apply(license);
            String key = expiration == null ? "unknown" : expiration.name();
            counts.put(key, counts.getOrDefault(key, 0L) + 1L);
        }
        return counts;
    }

    public Installation blockInstallation(UUID instanceId, String reason) {
        Installation inst = installationRepository.findById(instanceId).orElseThrow(() -> new IllegalArgumentException("Installation not found."));
        inst.setBlockedAt(Instant.now());
        inst.setBlockedReason(reason == null ? "Blocked by admin." : reason);
        inst.setLicenseStatus(InstallationStatus.BLOCKED);
        installationRepository.save(inst);
        return inst;
    }

    public Installation unblockInstallation(UUID instanceId) {
        Installation inst = installationRepository.findById(instanceId).orElseThrow(() -> new IllegalArgumentException("Installation not found."));
        inst.setBlockedAt(null);
        inst.setBlockedReason(null);
        inst.setLicenseStatus(InstallationStatus.UNLICENSED);
        installationRepository.save(inst);
        return inst;
    }
}
