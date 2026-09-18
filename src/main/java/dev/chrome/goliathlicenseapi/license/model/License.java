package dev.chrome.goliathlicenseapi.license.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "licenses")
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "license_key_hash", nullable = false, unique = true, length = 255)
    private String licenseKeyHash;

    @Column(name = "license_key_salt", nullable = false, length = 255)
    private String licenseKeySalt;

    @Column(name = "server_ip", nullable = false, length = 45)
    private String serverIp;

    @Column(name = "server_port", nullable = false)
    private Integer serverPort;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "expiration", nullable = false, length = 20)
    private LicenseExpiration expiration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LicenseStatus status;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = LicenseStatus.ACTIVE;
        }
    }

    public boolean isExpired(Instant now) {
        if (expiration == LicenseExpiration.NEVER) {
            return false;
        }
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLicenseKeyHash() {
        return licenseKeyHash;
    }

    public void setLicenseKeyHash(String licenseKeyHash) {
        this.licenseKeyHash = licenseKeyHash;
    }

    public String getLicenseKeySalt() {
        return licenseKeySalt;
    }

    public void setLicenseKeySalt(String licenseKeySalt) {
        this.licenseKeySalt = licenseKeySalt;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LicenseExpiration getExpiration() {
        return expiration;
    }

    public void setExpiration(LicenseExpiration expiration) {
        this.expiration = expiration;
    }

    public LicenseStatus getStatus() {
        return status;
    }

    public void setStatus(LicenseStatus status) {
        this.status = status;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public void setRevokedReason(String revokedReason) {
        this.revokedReason = revokedReason;
    }
}
