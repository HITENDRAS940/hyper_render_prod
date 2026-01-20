package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * Individual slot for a resource with its own timing and pricing.
 * These are pre-generated based on ResourceSlotConfig for fast access.
 */
@Entity
@Table(name = "resource_slots", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"resource_id", "start_time", "end_time"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private ServiceResource resource;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // Display name like "6:00 AM - 7:00 AM" or "Morning Slot 1"
    @Column
    private String displayName;

    // Base price for this specific slot (overrides config base price if set)
    @Column(nullable = false)
    private Double basePrice;

    // Weekday price (if different from base)
    @Column
    private Double weekdayPrice;

    // Weekend price (if different from base)
    @Column
    private Double weekendPrice;

    // Order for display purposes
    @Column
    private Integer displayOrder;

    // Whether this slot is enabled for booking
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    // Duration in minutes (calculated from start and end time)
    @Column(nullable = false)
    private Integer durationMinutes;
}

