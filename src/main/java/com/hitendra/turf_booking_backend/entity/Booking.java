package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_booking_resource_date", columnList = "resource_id, booking_date"),
    @Index(name = "idx_booking_service_date", columnList = "service_id, booking_date"),
    @Index(name = "idx_booking_status_lock_expires", columnList = "status, lock_expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private ServiceResource resource;

    /**
     * Activity code for which the booking was made.
     * Stored separately for analytics and validation.
     */
    @Column(name = "activity_code")
    private String activityCode;

    @Column(nullable = false)
    private java.time.LocalTime startTime;

    @Column(nullable = false)
    private java.time.LocalTime endTime;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(unique = true)
    private String reference;

    /**
     * Idempotency key for retry handling.
     * If a booking with this key already exists, the existing booking is returned.
     * Prevents duplicate bookings from frontend retries.
     */
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Lock expiry time for PAYMENT_PENDING bookings.
     * If payment is not confirmed before this time, booking will be marked as EXPIRED.
     * This implements soft-locking to prevent double booking during manual UPI payment.
     */
    @Column(name = "lock_expires_at")
    private Instant lockExpiresAt;

    /**
     * Payment mode for the booking (MANUAL_UPI, WALLET, ONLINE, etc.)
     */
    @Column(name = "payment_mode")
    private String paymentMode;

    @Enumerated(EnumType.STRING)
    private PaymentSource paymentSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_id")
    private AdminProfile adminProfile;

    // Razorpay payment tracking fields
    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature")
    private String razorpaySignature;

    @Column
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    @Builder.Default
    private PaymentStatus paymentStatusEnum = PaymentStatus.NOT_STARTED;

    @Column
    private Instant paymentTime;

    @Column(name = "payment_initiated_at")
    private Instant paymentInitiatedAt;

    /**
     * Amount paid online at the time of booking (X% of total).
     */
    @Column(name = "online_amount_paid", precision = 19, scale = 2)
    private java.math.BigDecimal onlineAmountPaid;

    /**
     * Amount due to be paid at the venue (remaining amount after online payment).
     */
    @Column(name = "venue_amount_due", precision = 19, scale = 2)
    private java.math.BigDecimal venueAmountDue;

    /**
     * Whether the venue amount has been collected.
     */
    @Column(name = "venue_amount_collected")
    @Builder.Default
    private Boolean venueAmountCollected = false;

    /**
     * Method used to collect venue amount (CASH or ONLINE).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "venue_payment_collection_method")
    private VenuePaymentCollectionMethod venuePaymentCollectionMethod;

    /**
     * Advance amount paid by user (calculated dynamically based on config at booking time).
     */
    @Column(name = "advance_amount", precision = 19, scale = 2)
    private java.math.BigDecimal advanceAmount;

    /**
     * Remaining amount to be paid at venue.
     */
    @Column(name = "remaining_amount", precision = 19, scale = 2)
    private java.math.BigDecimal remainingAmount;

    /**
     * Status of transfer of advance amount to venue.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_status")
    @Builder.Default
    private TransferStatus transferStatus = TransferStatus.PENDING;

    /**
     * Snapshot of the resource's pricing type at booking creation time.
     * Stored so historical bookings remain correct even if the resource
     * pricing type is changed later.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type")
    private com.hitendra.turf_booking_backend.entity.PricingType pricingType;

    /**
     * Number of persons for this booking.
     * Populated only for PER_PERSON bookings; null / 1 for PER_SLOT bookings.
     */
    @Column(name = "number_of_persons")
    private Integer numberOfPersons;
}
