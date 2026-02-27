package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a bookable resource within a service.
 * Examples: "Turf 1", "Court A", "Lane 1", "Pool 1"
 *
 * Slots are generated dynamically from slotConfig, NOT stored in database.
 */
@Entity
@Table(name = "service_resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(nullable = false)
    private String name;  // e.g., "Turf 1", "Turf 2", "Court A", "Lane 1"

    @Column
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * Pricing strategy for this resource.
     * PER_SLOT  - flat rate per slot (e.g. Turf).
     * PER_PERSON - price multiplied by number of persons (e.g. Bowling, PS5).
     * Defaults to PER_SLOT for backward compatibility.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false)
    @Builder.Default
    private PricingType pricingType = PricingType.PER_SLOT;

    /**
     * Maximum number of persons allowed per booking.
     * Only meaningful when pricingType = PER_PERSON.
     * Null means no upper limit (only @Min(1) is enforced on input).
     */
    @Column(name = "max_person_allowed")
    private Integer maxPersonAllowed;

    // Slot configuration for this resource (slots generated dynamically)
    @OneToOne(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private ResourceSlotConfig slotConfig;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "resource_activities",
        joinColumns = @JoinColumn(name = "resource_id"),
        inverseJoinColumns = @JoinColumn(name = "activity_id")
    )
    @Builder.Default
    private List<Activity> activities = new ArrayList<>();
}
