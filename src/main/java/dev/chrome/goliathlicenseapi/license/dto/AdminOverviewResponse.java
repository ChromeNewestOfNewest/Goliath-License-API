package dev.chrome.goliathlicenseapi.license.dto;

import java.util.Map;

public record AdminOverviewResponse(
        long totalServers,
        long onlineServers,
        long activeLicenses,
        long expiredLicenses,
        long unlicensedServers,
        long revokedLicenses,
        long totalInstallations,
        Map<String, Long> goliathVersionDistribution,
        Map<String, Long> minecraftVersionDistribution,
        Map<String, Long> licenseExpirationDistribution
) {}
