package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.appconfig.*;
import com.hitendra.turf_booking_backend.entity.AppConfig;
import com.hitendra.turf_booking_backend.repository.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppConfigService {

    private final AppConfigRepository appConfigRepository;

    // ─── Public ────────────────────────────────────────────────────────────────

    /**
     * Returns the first (and only expected) app config row.
     * Throws IllegalStateException when no config is seeded yet.
     */
    @Transactional(readOnly = true)
    public AppConfigResponse getAppConfig() {
        AppConfig config = appConfigRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("App configuration has not been set up yet."));
        return toResponse(config);
    }

    // ─── Manager CRUD ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AppConfigResponse> getAllConfigs() {
        return appConfigRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppConfigResponse getConfigById(Long id) {
        AppConfig config = findOrThrow(id);
        return toResponse(config);
    }

    public AppConfigResponse createConfig(AppConfigRequest request) {
        AppConfig config = AppConfig.builder()
                .iosMinVersion(request.getIosMinVersion())
                .iosLatestVersion(request.getIosLatestVersion())
                .iosStoreUrl(request.getIosStoreUrl())
                .androidMinVersion(request.getAndroidMinVersion())
                .androidLatestVersion(request.getAndroidLatestVersion())
                .androidStoreUrl(request.getAndroidStoreUrl())
                .forceUpdateMessage(request.getForceUpdateMessage())
                .softUpdateMessage(request.getSoftUpdateMessage())
                .build();
        AppConfig saved = appConfigRepository.save(config);
        log.info("AppConfig created with id={}", saved.getId());
        return toResponse(saved);
    }

    public AppConfigResponse updateConfig(Long id, AppConfigRequest request) {
        AppConfig config = findOrThrow(id);
        config.setIosMinVersion(request.getIosMinVersion());
        config.setIosLatestVersion(request.getIosLatestVersion());
        config.setIosStoreUrl(request.getIosStoreUrl());
        config.setAndroidMinVersion(request.getAndroidMinVersion());
        config.setAndroidLatestVersion(request.getAndroidLatestVersion());
        config.setAndroidStoreUrl(request.getAndroidStoreUrl());
        config.setForceUpdateMessage(request.getForceUpdateMessage());
        config.setSoftUpdateMessage(request.getSoftUpdateMessage());
        AppConfig saved = appConfigRepository.save(config);
        log.info("AppConfig updated for id={}", saved.getId());
        return toResponse(saved);
    }

    public void deleteConfig(Long id) {
        AppConfig config = findOrThrow(id);
        appConfigRepository.delete(config);
        log.info("AppConfig deleted for id={}", id);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private AppConfig findOrThrow(Long id) {
        return appConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AppConfig not found with id: " + id));
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

