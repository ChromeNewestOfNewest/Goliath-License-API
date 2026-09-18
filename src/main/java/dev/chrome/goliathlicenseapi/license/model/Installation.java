package dev.chrome.goliathlicenseapi.license.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "installations")
public class Installation {

    @Id
    @Column(name = "instance_id", nullable = false, updatable = false)
    private UUID instanceId;

    @Column(name = "observed_ip", nullable = false, length = 45)
    private String observedIp;

    @Column(name = "server_port", nullable = false)
    private Integer serverPort;

    @Column(name = "goliath_version", nullable = false, length = 100)
    private String goliathVersion;

    @Column(name = "minecraft_version", nullable = false, length = 100)
    private String minecraftVersion;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @Column(name = "last_successful_validation")
    private Instant lastSuccessfulValidation;

    @Enumerated(EnumType.STRING)
    @Column(name = "license_status", nullable = false, length = 20)
    private InstallationStatus licenseStatus;

    @Column(name = "associated_license_id")
    private UUID associatedLicenseId;

    public UUID getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(UUID instanceId) {
        this.instanceId = instanceId;
    }

    public String getObservedIp() {
        return observedIp;
    }

    public void setObservedIp(String observedIp) {
        this.observedIp = observedIp;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public String getGoliathVersion() {
        return goliathVersion;
    }

    public void setGoliathVersion(String goliathVersion) {
        this.goliathVersion = goliathVersion;
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(Instant firstSeen) {
        this.firstSeen = firstSeen;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Instant getLastSuccessfulValidation() {
        return lastSuccessfulValidation;
    }

    public void setLastSuccessfulValidation(Instant lastSuccessfulValidation) {
        this.lastSuccessfulValidation = lastSuccessfulValidation;
    }

    public InstallationStatus getLicenseStatus() {
        return licenseStatus;
    }

    public void setLicenseStatus(InstallationStatus licenseStatus) {
        this.licenseStatus = licenseStatus;
    }

    public UUID getAssociatedLicenseId() {
        return associatedLicenseId;
    }

    public void setAssociatedLicenseId(UUID associatedLicenseId) {
        this.associatedLicenseId = associatedLicenseId;
    }
}
