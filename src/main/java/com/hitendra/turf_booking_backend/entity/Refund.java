package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity to track refund records for cancelled bookings.
 * Supports both Razorpay refunds and wallet refunds.
 */
@Entity
@Table(name = "refunds", indexes = {
    @Index(name = "idx_refund_booking", columnList = "booking_id"),
    @Index(name = "idx_refund_razorpay_id", columnList = "razorpay_refund_id"),
    @Index(name = "idx_refund_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Original amount paid for the booking
     */
    @Column(name = "original_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;

    /**
     * Amount to be refunded (after applying refund rules)
     */
    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;

    /**
     * Percentage of refund applied
     */
    @Column(name = "refund_percent", nullable = false)
    private Integer refundPercent;

    /**
     * Minutes before slot when cancellation was made
     */
    @Column(name = "minutes_before_slot", nullable = false)
    private Long minutesBeforeSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RefundStatus status = RefundStatus.INITIATED;

    /**
     * Razorpay refund ID (if refunded via Razorpay)
     */
    @Column(name = "razorpay_refund_id")
    private String razorpayRefundId;

    /**
     * Razorpay payment ID being refunded
     */
    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    /**
     * Wallet transaction ID (if refunded to wallet)
     */
    @Column(name = "wallet_transaction_id")
    private Long walletTransactionId;

    /**
     * Type of refund - RAZORPAY or WALLET
     */
    @Column(name = "refund_type", nullable = false)
    private String refundType;

    /**
     * Reason for cancellation/refund
     */
    @Column(name = "reason")
    private String reason;

    /**
     * When refund was initiated
     */
    @Column(name = "initiated_at", nullable = false)
    @Builder.Default
    private Instant initiatedAt = Instant.now();

    /**
     * When refund was processed/completed
     */
    @Column(name = "processed_at")
    private Instant processedAt;

    /**
     * Error message if refund failed
     */
    @Column(name = "error_message")
    private String errorMessage;

    /**
     * Currency of the refund
     */
    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "INR";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Service venue;
}
