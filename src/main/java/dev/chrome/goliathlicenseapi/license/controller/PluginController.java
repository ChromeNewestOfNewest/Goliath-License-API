package dev.chrome.goliathlicenseapi.license.controller;

import dev.chrome.goliathlicenseapi.license.dto.PluginHeartbeatRequest;
import dev.chrome.goliathlicenseapi.license.dto.PluginHeartbeatResponse;
import dev.chrome.goliathlicenseapi.license.dto.PluginValidateRequest;
import dev.chrome.goliathlicenseapi.license.dto.PluginValidateResponse;
import dev.chrome.goliathlicenseapi.license.dto.PluginActionClaimRequest;
import dev.chrome.goliathlicenseapi.license.dto.PluginActionPollResponse;
import dev.chrome.goliathlicenseapi.license.dto.PluginActionReportRequest;
import dev.chrome.goliathlicenseapi.license.service.PluginService;
import dev.chrome.goliathlicenseapi.license.service.RemoteActionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plugin")
public class PluginController {

    private final PluginService pluginService;
    private final RemoteActionService remoteActionService;

    public PluginController(PluginService pluginService, RemoteActionService remoteActionService) {
        this.pluginService = pluginService;
        this.remoteActionService = remoteActionService;
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

    @PostMapping("/actions/poll")
    public ResponseEntity<PluginActionPollResponse> pollAction(@Valid @RequestBody PluginActionClaimRequest request, HttpServletRequest servletRequest) {
        String observedIp = servletRequest.getHeader("X-Forwarded-For") == null ? servletRequest.getRemoteAddr() : servletRequest.getHeader("X-Forwarded-For").split(",")[0].trim();
        PluginActionPollResponse resp = remoteActionService.pollForInstance(request.instanceId(), request.licenseKey(), observedIp);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/actions/claim")
    public ResponseEntity<Map<String,Object>> claimAction(@Valid @RequestBody PluginActionClaimRequest request, HttpServletRequest servletRequest) {
        String observedIp = servletRequest.getHeader("X-Forwarded-For") == null ? servletRequest.getRemoteAddr() : servletRequest.getHeader("X-Forwarded-For").split(",")[0].trim();
        boolean claimed = remoteActionService.claimAction(request.instanceId(), request, observedIp);
        Map<String,Object> resp = new java.util.LinkedHashMap<>();
        resp.put("claimed", claimed);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/actions/report")
    public ResponseEntity<Map<String,Object>> reportAction(@Valid @RequestBody PluginActionReportRequest request, HttpServletRequest servletRequest) {
        String observedIp = servletRequest.getHeader("X-Forwarded-For") == null ? servletRequest.getRemoteAddr() : servletRequest.getHeader("X-Forwarded-For").split(",")[0].trim();
        remoteActionService.reportResult(request, observedIp);
        Map<String,Object> resp = new java.util.LinkedHashMap<>();
        resp.put("status", "OK");
        return ResponseEntity.ok(resp);
    }
}
