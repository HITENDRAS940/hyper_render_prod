package com.hitendra.turf_booking_backend.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal coupon list item — only id, code, and description.
 * Used by both the public listing endpoint and the admin list view.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponSummaryDto {
    private Long id;
    private String code;
    private String description;
}
