package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ADMIN LEDGER - Append-only ledger for admin cash/bank transactions
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Keeps a running-balance trail for each admin, split by ledger type:
 *  CASH  → physical money collected or spent
 *  BANK  → digital money (UPI/bank transfer)
 *
 * Every balance-changing event (venue collection, settlement, expense) must
 * create an entry here so the admin can see a full statement.
 *
 * CREDIT = money IN  (increases balance)
 * DEBIT  = money OUT (decreases balance)
 */
@Entity
@Table(name = "admin_ledger", indexes = {
    @Index(name = "idx_admin_ledger_admin_type",    columnList = "admin_profile_id, ledger_type"),
    @Index(name = "idx_admin_ledger_admin_created",  columnList = "admin_profile_id, created_at"),
    @Index(name = "idx_admin_ledger_ref",            columnList = "reference_type, reference_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_profile_id", nullable = false)
    private AdminProfile admin;

    /** Which sub-ledger: CASH or BANK */
    @Enumerated(EnumType.STRING)
    @Column(name = "ledger_type", nullable = false, length = 10)
    private AdminLedgerType ledgerType;

    /** CREDIT (money in) or DEBIT (money out) */
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private AdminLedgerEntryType entryType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Running balance of the sub-ledger AFTER this entry. */
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    /** Human-readable description (e.g., "Venue cash collected for BK-XYZ"). */
    @Column(length = 500)
    private String description;

    /**
     * Category of event that triggered this entry.
     * E.g., BOOKING, SETTLEMENT, EXPENSE, ADJUSTMENT
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /** ID of the source record (booking id, settlement id, expense id, …). */
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

