package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.appconfig.AppConfigResponse;
import com.hitendra.turf_booking_backend.service.AppConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/app")
@RequiredArgsConstructor
@Tag(name = "App Config", description = "Public endpoint to retrieve app update configuration")
public class AppConfigController {

    private final AppConfigService appConfigService;

    @GetMapping("/config")
    @Operation(
            summary = "Get app update config",
            description = "Returns the current app update configuration including minimum and latest versions for iOS and Android, store URLs, and update messages."
    )
    public ResponseEntity<AppConfigResponse> getAppConfig() {
        return ResponseEntity.ok(appConfigService.getAppConfig());
    }
}

