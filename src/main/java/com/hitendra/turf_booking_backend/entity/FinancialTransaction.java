package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable audit log of every financial event per admin.
 *
 * Types:
 *  - ADVANCE_ONLINE  → bookingId (platform collected, pending)
 *  - VENUE_CASH      → bookingId (admin cash)
 *  - VENUE_BANK      → bookingId (admin bank/UPI direct)
 *  - SETTLEMENT      → settlementId (manager → admin bank transfer)
 */
@Entity
@Table(name = "financial_transactions", indexes = {
    @Index(name = "idx_fin_tx_admin", columnList = "admin_id"),
    @Index(name = "idx_fin_tx_type_ref", columnList = "type, reference_id"),
    @Index(name = "idx_fin_tx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private AdminProfile admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialTransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** BookingId for booking-related types; SettlementId for SETTLEMENT. */
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

