package com.hitendra.turf_booking_backend.dto.appconfig;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformVersionDto {
    private String minVersion;
    private String latestVersion;
    private String storeUrl;
}

