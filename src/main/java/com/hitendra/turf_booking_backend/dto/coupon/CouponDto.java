package com.hitendra.turf_booking_backend.dto.coupon;

import com.hitendra.turf_booking_backend.entity.DayType;
import com.hitendra.turf_booking_backend.entity.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDto {

    // ── Identity ─────────────────────────────────────────────────────────────
    private Long id;
    private String code;
    private String description;

    // ── Discount ─────────────────────────────────────────────────────────────
    private DiscountType discountType;
    private Double discountValue;
    private Double minBookingAmount;
    private Double maxDiscountAmount;

    // ── Validity window ───────────────────────────────────────────────────────
    private LocalDate validFrom;
    private LocalDate expiryDate;

    // ── Usage limits ─────────────────────────────────────────────────────────
    private boolean active;
    private Integer usageLimit;
    private Integer currentUsage;
    private Integer perUserUsageLimit;

    // ── User constraints ─────────────────────────────────────────────────────
    private boolean newUsersOnly;

    // ── Scope constraints ─────────────────────────────────────────────────────
    private Set<Long> applicableServiceIds;
    private Set<Long> applicableResourceIds;
    private Set<String> applicableActivityCodes;

    // ── Booking constraints ───────────────────────────────────────────────────
    private Integer minBookingDurationMinutes;
    private DayType validDayType;

    // ── Audit ─────────────────────────────────────────────────────────────────
    private Instant createdAt;
}
