package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupon_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identity ─────────────────────────────────────────────────────────────

    @Column(nullable = false, unique = true)
    private String code;

    /** Human-readable promo description shown to the user (e.g. "20% off your first booking!") */
    @Column(length = 500)
    private String description;

    // ── Discount ─────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private Double discountValue;

    /** Minimum booking amount required to apply this coupon. */
    @Column(name = "min_booking_amount")
    private Double minBookingAmount;

    /** Cap for percentage discounts — the discount will not exceed this amount. */
    @Column(name = "max_discount_amount")
    private Double maxDiscountAmount;

    // ── Validity window ───────────────────────────────────────────────────────

    /** Date from which the coupon becomes active (inclusive). NULL = active immediately. */
    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate expiryDate;

    // ── Usage limits ─────────────────────────────────────────────────────────

    @Builder.Default
    private boolean active = true;

    /** Total redemptions allowed across ALL users. NULL = unlimited. */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Builder.Default
    @Column(name = "current_usage")
    private Integer currentUsage = 0;

    /**
     * Maximum number of times a SINGLE user may redeem this coupon.
     * NULL = 1 (one-time per user, the most common default).
     * Set > 1 for loyalty / multi-use coupons.
     */
    @Column(name = "per_user_usage_limit")
    @Builder.Default
    private Integer perUserUsageLimit = 1;

    // ── User constraints ─────────────────────────────────────────────────────

    /**
     * When true, only users who have NEVER completed a booking before may use this coupon.
     * Useful for "first booking" acquisition campaigns.
     */
    @Column(name = "new_users_only", nullable = false)
    @Builder.Default
    private boolean newUsersOnly = false;

    // ── Scope constraints ─────────────────────────────────────────────────────

    /**
     * If non-empty, the coupon is valid ONLY for bookings at these services.
     * Empty = valid for ALL services.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "coupon_applicable_services",
            joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "service_id")
    @Builder.Default
    private Set<Long> applicableServiceIds = new HashSet<>();

    /**
     * If non-empty, the coupon is valid ONLY for bookings on these specific resources.
     * Empty = valid for ALL resources.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "coupon_applicable_resources",
            joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "resource_id")
    @Builder.Default
    private Set<Long> applicableResourceIds = new HashSet<>();

    /**
     * If non-empty, the coupon is valid ONLY for bookings of these activity types.
     * E.g. ["CRICKET", "FOOTBALL"]. Empty = valid for ALL activities.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "coupon_applicable_activities",
            joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "activity_code")
    @Builder.Default
    private Set<String> applicableActivityCodes = new HashSet<>();

    // ── Booking constraints ───────────────────────────────────────────────────

    /**
     * Minimum booking slot duration in minutes required to apply this coupon.
     * E.g. 60 = only bookings of at least 1 hour qualify. NULL = no restriction.
     */
    @Column(name = "min_booking_duration_minutes")
    private Integer minBookingDurationMinutes;

    /**
     * Day-type restriction: WEEKDAY, WEEKEND, or ALL (null = ALL).
     * Lets you create "Weekend special" or "Midweek deal" coupons.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "valid_day_type")
    private DayType validDayType;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @Builder.Default
    private Instant createdAt = Instant.now();
}
