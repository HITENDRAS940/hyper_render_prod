package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.appconfig.*;
import com.hitendra.turf_booking_backend.entity.AppConfig;
import com.hitendra.turf_booking_backend.repository.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppConfigService {

    private final AppConfigRepository appConfigRepository;

    // ─── Public ────────────────────────────────────────────────────────────────

    /**
     * Returns the single app-config row.
     * Throws if no config has been seeded yet.
     */
    @Transactional(readOnly = true)
    public AppConfigResponse getAppConfig() {
        return toResponse(getSingleton());
    }

    // ─── Manager ───────────────────────────────────────────────────────────────

    /**
     * Updates the single app-config row in-place.
     * Never creates a second row.
     */
    public AppConfigResponse updateConfig(AppConfigRequest request) {
        AppConfig config = getSingleton();
        config.setIosMinVersion(request.getIosMinVersion());
        config.setIosLatestVersion(request.getIosLatestVersion());
        config.setIosStoreUrl(request.getIosStoreUrl());
        config.setAndroidMinVersion(request.getAndroidMinVersion());
        config.setAndroidLatestVersion(request.getAndroidLatestVersion());
        config.setAndroidStoreUrl(request.getAndroidStoreUrl());
        config.setForceUpdateMessage(request.getForceUpdateMessage());
        config.setSoftUpdateMessage(request.getSoftUpdateMessage());
        AppConfig saved = appConfigRepository.save(config);
        log.info("AppConfig updated (id={})", saved.getId());
        return toResponse(saved);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private AppConfig getSingleton() {
        return appConfigRepository.findSingleton()
                .orElseThrow(() -> new IllegalStateException("App configuration has not been seeded yet."));
    }

    private AppConfigResponse toResponse(AppConfig config) {
        PlatformVersionDto ios = PlatformVersionDto.builder()
                .minVersion(config.getIosMinVersion())
                .latestVersion(config.getIosLatestVersion())
                .storeUrl(config.getIosStoreUrl())
                .build();

        PlatformVersionDto android = PlatformVersionDto.builder()
                .minVersion(config.getAndroidMinVersion())
                .latestVersion(config.getAndroidLatestVersion())
                .storeUrl(config.getAndroidStoreUrl())
                .build();

        UpdateConfigDto updateConfig = UpdateConfigDto.builder()
                .ios(ios)
                .android(android)
                .forceUpdateMessage(config.getForceUpdateMessage())
                .softUpdateMessage(config.getSoftUpdateMessage())
                .build();

        return AppConfigResponse.builder()
                .updateConfig(updateConfig)
                .build();
    }
}
