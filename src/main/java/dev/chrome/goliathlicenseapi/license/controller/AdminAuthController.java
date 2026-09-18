package dev.chrome.goliathlicenseapi.license.controller;

import dev.chrome.goliathlicenseapi.license.dto.AdminLoginRequest;
import dev.chrome.goliathlicenseapi.license.dto.AdminLoginResponse;
import dev.chrome.goliathlicenseapi.license.dto.AdminMeResponse;
import dev.chrome.goliathlicenseapi.license.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(adminAuthService.login(request, servletRequest));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest servletRequest) {
        adminAuthService.logout(servletRequest);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/auth/me")
    public ResponseEntity<AdminMeResponse> me() {
        return ResponseEntity.ok(adminAuthService.me());
    }
}
