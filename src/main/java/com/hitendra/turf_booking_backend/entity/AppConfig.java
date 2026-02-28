package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // iOS versions
    @Column(name = "ios_min_version", nullable = false)
    private String iosMinVersion;

    @Column(name = "ios_latest_version", nullable = false)
    private String iosLatestVersion;

    @Column(name = "ios_store_url", nullable = false, length = 500)
    private String iosStoreUrl;

    // Android versions
    @Column(name = "android_min_version", nullable = false)
    private String androidMinVersion;

    @Column(name = "android_latest_version", nullable = false)
    private String androidLatestVersion;

    @Column(name = "android_store_url", nullable = false, length = 500)
    private String androidStoreUrl;

    // Update messages
    @Column(name = "force_update_message", nullable = false, length = 1000)
    private String forceUpdateMessage;

    @Column(name = "soft_update_message", nullable = false, length = 1000)
    private String softUpdateMessage;
}

