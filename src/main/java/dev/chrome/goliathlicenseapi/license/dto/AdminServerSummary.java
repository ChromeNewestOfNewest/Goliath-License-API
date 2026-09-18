package dev.chrome.goliathlicenseapi.license.dto;

import dev.chrome.goliathlicenseapi.license.model.InstallationStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminServerSummary(
        UUID instanceId,
        String observedIp,
        Integer serverPort,
        String goliathVersion,
        String minecraftVersion,
        Instant firstSeen,
        Instant lastSeen,
        InstallationStatus licenseStatus,
        UUID associatedLicenseId,
        boolean online
) {}
