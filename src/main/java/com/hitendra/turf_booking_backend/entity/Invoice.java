package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoice_booking_id", columnList = "booking_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The booking this invoice belongs to — one-to-one via booking_id */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    /** URL of the generated invoice PDF/document */
    @Column(name = "invoice_url", nullable = false, length = 1024)
    private String invoiceUrl;

    // ── Snapshot fields copied from Booking at receipt time ──────────────────

    @Column(name = "booking_reference")
    private String bookingReference;

    @Column(name = "booking_date")
    private LocalDate bookingDate;

    @Column(name = "start_time")
    private java.time.LocalTime startTime;

    @Column(name = "end_time")
    private java.time.LocalTime endTime;

    /** Total booking amount */
    @Column(name = "total_amount")
    private Double totalAmount;

    /** Amount paid online (advance) */
    @Column(name = "online_amount_paid", precision = 19, scale = 2)
    private BigDecimal onlineAmountPaid;

    /** Amount due at venue */
    @Column(name = "venue_amount_due", precision = 19, scale = 2)
    private BigDecimal venueAmountDue;

    /** Payment method used (e.g. card, upi, netbanking) */
    @Column(name = "payment_method")
    private String paymentMethod;

    /** Payment mode stored on booking (e.g. ONLINE, MANUAL_UPI) */
    @Column(name = "payment_mode")
    private String paymentMode;

    /** Razorpay payment ID */
    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    // ── Service / Resource snapshot ──────────────────────────────────────────

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "resource_name")
    private String resourceName;

    // ── User snapshot ─────────────────────────────────────────────────────────

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "user_phone")
    private String userPhone;

    /** Timestamp when this invoice record was created */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

