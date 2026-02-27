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

    @Column(length = 50)
    private String contactNumber;

    @Column(name = "gstin")
    private String gstin;

    @Column(name = "state")
    private String state;

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

    /**
     * Whether cancellation (and refund) is allowed for bookings under this service.
     * When false, any user-initiated cancellation request is rejected immediately.
     * Defaults to true so existing services continue to honour cancellations.
     */
    @Column(name = "refund_allowed", nullable = false)
    @Builder.Default
    private boolean refundAllowed = true;

    // Google Places API fields
    @Column(name = "google_place_id")
    private String googlePlaceId;

    @Column(name = "google_rating")
    private Double googleRating;

    @Column(name = "google_review_count")
    private Integer googleReviewCount;

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
