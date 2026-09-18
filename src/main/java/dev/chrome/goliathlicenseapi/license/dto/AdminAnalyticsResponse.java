package dev.chrome.goliathlicenseapi.license.dto;

import java.util.Map;

public record AdminAnalyticsResponse(
        Map<String, Long> licenseCreationByDay,
        Map<String, Long> licenseExpirationDistribution,
        Map<String, Long> goliathVersionDistribution,
        Map<String, Long> minecraftVersionDistribution,
        long totalLicenses,
        long activeLicenses,
        long expiredLicenses,
        long revokedLicenses,
        long totalInstallations,
        long onlineInstallations
) {}
