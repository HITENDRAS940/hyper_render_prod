package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.appconfig.AppConfigResponse;
import com.hitendra.turf_booking_backend.dto.coupon.CouponSummaryDto;
import com.hitendra.turf_booking_backend.service.AppConfigService;
import com.hitendra.turf_booking_backend.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/app")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Public endpoints — no authentication required")
public class AppConfigController {

    private final AppConfigService appConfigService;
    private final CouponService couponService;

    @GetMapping("/config")
    @Operation(
            summary = "Get app update config",
            description = "Returns the current app update configuration including minimum and latest versions for iOS and Android, store URLs, and update messages."
    )
    public ResponseEntity<AppConfigResponse> getAppConfig() {
        return ResponseEntity.ok(appConfigService.getAppConfig());
    }

    @GetMapping("/coupons")
    @Operation(
            summary = "Get available coupons",
            description = "Returns all currently active and valid coupons (code + description). " +
                    "No authentication required. Expired, inactive, or not-yet-active coupons are excluded."
    )
    public ResponseEntity<List<CouponSummaryDto>> getAvailableCoupons() {
        return ResponseEntity.ok(couponService.getAvailableCoupons());
    }
}


