package com.hitendra.turf_booking_backend.dto.appconfig;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateConfigDto {
    private PlatformVersionDto ios;
    private PlatformVersionDto android;
    private String forceUpdateMessage;
    private String softUpdateMessage;
}

