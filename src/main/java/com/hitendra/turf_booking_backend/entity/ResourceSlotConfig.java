package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for generating slots dynamically for a specific resource.
 * Defines the operating hours, slot duration, and base pricing.
 *
 * Slots are NOT stored in database - they are generated dynamically using:
 * - openingTime, closingTime, slotDurationMinutes for slot times
 * - basePrice for default pricing
 * - priceRules for dynamic pricing (weekends, peak hours, etc.)
 */
@Entity
@Table(name = "resource_slot_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceSlotConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false, unique = true)
    private ServiceResource resource;

    // Operating hours - when slots start and end
    @Column(nullable = false)
    private LocalTime openingTime;  // e.g., 06:00

    @Column(nullable = false)
    private LocalTime closingTime;  // e.g., 23:00

    // Duration of each slot in minutes
    @Column(nullable = false)
    private Integer slotDurationMinutes;  // e.g., 60 for 1 hour slots

    // Base price for all generated slots
    @Column(nullable = false)
    private Double basePrice;

    // Price rules for dynamic pricing (weekends, peak hours, etc.)
    @OneToMany(mappedBy = "resourceSlotConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ResourcePriceRule> priceRules = new ArrayList<>();

    // Whether this config is active
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * Calculate the number of slots that will be generated.
     */
    public int getSlotCount() {
        if (openingTime == null || closingTime == null || slotDurationMinutes == null || slotDurationMinutes <= 0) {
            return 0;
        }
        int totalMinutes = (closingTime.toSecondOfDay() - openingTime.toSecondOfDay()) / 60;
        return totalMinutes / slotDurationMinutes;
    }
}
