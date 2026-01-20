package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String location;

    @Column
    private String city;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private String description;

    @Column
    private String contactNumber;

    @Column
    private LocalTime startTime;

    @Column
    private LocalTime endTime;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_amenities", joinColumns = @JoinColumn(name = "service_id"))
    @Column(name = "amenity")
    private List<String> amenities;

    @Column
    @Builder.Default
    private boolean availability = true;

    // Track which admin created this service
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_id")
    private AdminProfile createdBy;

    // Store image URLs
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_images", joinColumns = @JoinColumn(name = "service_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "service_activity",
        joinColumns = @JoinColumn(name = "service_id"),
        inverseJoinColumns = @JoinColumn(name = "activity_id")
    )
    @Builder.Default
    private List<Activity> activities = new ArrayList<>();

    // One-to-many relationship with ServiceResource (e.g., Turf 1, Turf 2, Court A)
    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ServiceResource> resources = new ArrayList<>();
}
