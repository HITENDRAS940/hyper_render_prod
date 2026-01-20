package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * Dynamic pricing rules for a resource.
 * Rules are applied based on day type, time range, and priority.
 * Higher priority rules win when multiple rules match.
 */
@Entity
@Table(name = "resource_price_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourcePriceRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_slot_config_id", nullable = false)
    private ResourceSlotConfig resourceSlotConfig;

    // Which days this rule applies to
    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false)
    @Builder.Default
    private DayType dayType = DayType.ALL;

    // Time range for this rule
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // Base price for slots in this time range (overrides slot base price if set)
    @Column(name = "base_price")
    private Double basePrice;

    // Extra charge to add on top of base price
    @Column(name = "extra_charge")
    @Builder.Default
    private Double extraCharge = 0.0;

    // Reason for this pricing rule (e.g., "Night lighting", "Peak hours", "Early bird discount")
    @Column
    private String reason;

    // Priority - higher value wins when multiple rules match
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 1;

    // Whether this rule is active
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
}

