package com.hitendra.turf_booking_backend.entity.accounting;

import com.hitendra.turf_booking_backend.entity.Service;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * CASH LEDGER - THE SINGLE SOURCE OF TRUTH FOR ALL MONEY MOVEMENT
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * CRITICAL RULES:
 * 1. APPEND-ONLY: Never update or delete entries
 * 2. Every rupee IN or OUT must be recorded here
 * 3. Balance is calculated cumulatively
 * 4. Used for financial reconciliation and audit trail
 *
 * SOURCES OF ENTRIES:
 * - BOOKING: Slot booking revenue (credit)
 * - INVENTORY_SALE: Cafe/shop sales (credit)
 * - EXPENSE: Bills, maintenance, salaries (debit)
 * - INVENTORY_PURCHASE: Stock purchases (debit)
 * - REFUND: Customer refunds (debit)
 * - ADJUSTMENT: Manual corrections (credit or debit)
 *
 * CREDIT = Money IN (increases balance)
 * DEBIT = Money OUT (decreases balance)
 */
@Entity
@Table(name = "cash_ledger", indexes = {
    @Index(name = "idx_service_created", columnList = "service_id, created_at"),
    @Index(name = "idx_source_reference", columnList = "source, reference_type, reference_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReferenceType referenceType;

    @Column(nullable = false)
    private Long referenceId; // ID of the source record (booking, sale, expense, etc.)

    @Column
    private Double creditAmount; // Money IN

    @Column
    private Double debitAmount; // Money OUT

    @Column(nullable = false)
    private Double balanceAfter; // Running balance after this transaction

    @Enumerated(EnumType.STRING)
    @Column
    private PaymentMode paymentMode;

    @Column
    private String description;

    @Column
    private String recordedBy; // User/Admin who recorded this

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();

        // Ensure exactly one of credit/debit is set
        if (creditAmount == null) {
            creditAmount = 0.0;
        }
        if (debitAmount == null) {
            debitAmount = 0.0;
        }
    }

    /**
     * Calculate net effect of this transaction.
     * Positive = Money IN, Negative = Money OUT
     */
    public Double getNetEffect() {
        return creditAmount - debitAmount;
    }
}

