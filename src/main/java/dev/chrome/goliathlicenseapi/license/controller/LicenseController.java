package dev.chrome.goliathlicenseapi.license.controller;

import dev.chrome.goliathlicenseapi.license.dto.GenerateLicenseRequest;
import dev.chrome.goliathlicenseapi.license.dto.GenerateLicenseResponse;
import dev.chrome.goliathlicenseapi.license.dto.LicenseSummaryResponse;
import dev.chrome.goliathlicenseapi.license.dto.RevokeLicenseRequest;
import dev.chrome.goliathlicenseapi.license.dto.RevokeLicenseResponse;
import dev.chrome.goliathlicenseapi.license.dto.ValidateLicenseRequest;
import dev.chrome.goliathlicenseapi.license.dto.ValidateLicenseResponse;
import dev.chrome.goliathlicenseapi.license.service.LicenseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/licenses")
@Validated
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @PostMapping
    public ResponseEntity<GenerateLicenseResponse> generateLicense(@Valid @RequestBody GenerateLicenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(licenseService.generateLicense(request));
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateLicenseResponse> validateLicense(@Valid @RequestBody ValidateLicenseRequest request) {
        return ResponseEntity.ok(licenseService.validateLicense(request));
    }

    @PostMapping("/revoke")
    public ResponseEntity<RevokeLicenseResponse> revokeLicense(@Valid @RequestBody RevokeLicenseRequest request) {
        return ResponseEntity.ok(licenseService.revokeLicense(request));
    }

    @GetMapping
    public ResponseEntity<List<LicenseSummaryResponse>> listLicenses() {
        return ResponseEntity.ok(licenseService.listLicenses());
    }
}
