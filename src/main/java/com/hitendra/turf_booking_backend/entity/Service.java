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

    /**
     * Percentage of the total booking amount the user must pay online upfront.
     * The remainder is collected at the venue.
     * When null, the global {@code pricing.online-payment-percent} config value is used.
     */
    @Column(name = "online_payment_percent")
    private Double onlinePaymentPercent;

    /**
     * Service-specific terms and conditions.
     * Stored as TEXT in the database to support long-form content (1000+ words).
     * Admins can set this per service. Users should be shown and asked to accept
     * these terms before completing a booking.
     * Null means the service has no custom terms and conditions.
     */
    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    /**
     * Determines how resources are selected during booking.
     *
     * AUTO (default):
     *   - Backend automatically allocates resource using priority algorithm.
     *   - Frontend shows aggregated slots without exposing individual resource IDs.
     *   - Resources with the same activity + price are pooled together.
     *   - Best for: Turfs, courts where resources are interchangeable.
     *
     * MANUAL:
     *   - User explicitly selects a resource before booking.
     *   - Frontend shows individual resources with their slot availability.
     *   - resourceId must be supplied in the booking request.
     *   - Best for: Bowling lanes, arcade machines, or any service where
     *     resource identity matters to the user.
     *
     * Defaults to AUTO for backward compatibility with all existing services.
     */
    @Column(name = "resource_selection_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ResourceSelectionMode resourceSelectionMode = ResourceSelectionMode.AUTO;

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
