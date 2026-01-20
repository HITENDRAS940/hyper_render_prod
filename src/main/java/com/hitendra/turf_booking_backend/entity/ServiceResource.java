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
