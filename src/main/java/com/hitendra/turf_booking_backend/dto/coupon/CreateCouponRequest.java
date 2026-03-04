package com.hitendra.turf_booking_backend.dto.coupon;

import com.hitendra.turf_booking_backend.entity.DayType;
import com.hitendra.turf_booking_backend.entity.DiscountType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCouponRequest {

    // ── Identity ─────────────────────────────────────────────────────────────

    @NotBlank(message = "Coupon code is required")
    private String code;

    /** Optional human-readable description shown to the user. */
    private String description;

    // ── Discount ─────────────────────────────────────────────────────────────

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be positive")
    private Double discountValue;

    @PositiveOrZero(message = "Minimum booking amount must be >= 0")
    private Double minBookingAmount;

    /** Cap on percentage discounts. Ignored for FIXED discounts. */
    @PositiveOrZero(message = "Max discount amount must be >= 0")
    private Double maxDiscountAmount;

    // ── Validity window ───────────────────────────────────────────────────────

    /** Date from which the coupon becomes usable. NULL = usable immediately. */
    private LocalDate validFrom;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;

    // ── Usage limits ─────────────────────────────────────────────────────────

    /** Total redemptions across all users. NULL = unlimited. */
    @Positive(message = "Usage limit must be positive")
    private Integer usageLimit;

    /**
     * How many times a single user may use this coupon.
     * Defaults to 1 (one-time per user) when not provided.
     */
    @Positive(message = "Per-user usage limit must be positive")
    private Integer perUserUsageLimit;

    // ── User constraints ─────────────────────────────────────────────────────

    /**
     * When true, only users with zero prior confirmed bookings may use this coupon.
     */
    @Builder.Default
    private boolean newUsersOnly = false;

    // ── Scope constraints ─────────────────────────────────────────────────────

    /** Service IDs this coupon is limited to. Empty = all services. */
    @Builder.Default
    private Set<Long> applicableServiceIds = new HashSet<>();

    /** Resource IDs this coupon is limited to. Empty = all resources. */
    @Builder.Default
    private Set<Long> applicableResourceIds = new HashSet<>();

    /** Activity codes this coupon is limited to (e.g. "CRICKET"). Empty = all activities. */
    @Builder.Default
    private Set<String> applicableActivityCodes = new HashSet<>();

    // ── Booking constraints ───────────────────────────────────────────────────

    /** Minimum booking duration in minutes required. NULL = no restriction. */
    @Positive(message = "Min booking duration must be positive")
    private Integer minBookingDurationMinutes;

    /**
     * Day-type restriction: WEEKDAY, WEEKEND, or ALL.
     * NULL is treated as ALL.
     */
    private DayType validDayType;
}
