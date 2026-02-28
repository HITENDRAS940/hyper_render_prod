package com.hitendra.turf_booking_backend.dto.appconfig;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfigRequest {

    @NotBlank(message = "iOS minimum version is required")
    private String iosMinVersion;

    @NotBlank(message = "iOS latest version is required")
    private String iosLatestVersion;

    @NotBlank(message = "iOS store URL is required")
    private String iosStoreUrl;

    @NotBlank(message = "Android minimum version is required")
    private String androidMinVersion;

    @NotBlank(message = "Android latest version is required")
    private String androidLatestVersion;

    @NotBlank(message = "Android store URL is required")
    private String androidStoreUrl;

    @NotBlank(message = "Force update message is required")
    private String forceUpdateMessage;

    @NotBlank(message = "Soft update message is required")
    private String softUpdateMessage;
}


