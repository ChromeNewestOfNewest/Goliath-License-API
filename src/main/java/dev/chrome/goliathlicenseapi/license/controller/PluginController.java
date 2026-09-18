package dev.chrome.goliathlicenseapi.license.controller;

import dev.chrome.goliathlicenseapi.license.dto.PluginHeartbeatRequest;
import dev.chrome.goliathlicenseapi.license.dto.PluginHeartbeatResponse;
import dev.chrome.goliathlicenseapi.license.dto.PluginValidateRequest;
import dev.chrome.goliathlicenseapi.license.dto.PluginValidateResponse;
import dev.chrome.goliathlicenseapi.license.service.PluginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plugin")
public class PluginController {

    private final PluginService pluginService;

    public PluginController(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    @PostMapping("/validate")
    public ResponseEntity<PluginValidateResponse> validate(@Valid @RequestBody PluginValidateRequest request, HttpServletRequest servletRequest) {
        PluginValidateResponse resp = pluginService.validate(request, servletRequest);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<PluginHeartbeatResponse> heartbeat(@Valid @RequestBody PluginHeartbeatRequest request, HttpServletRequest servletRequest) {
        PluginHeartbeatResponse resp = pluginService.heartbeat(request, servletRequest);
        return ResponseEntity.ok(resp);
    }
}
