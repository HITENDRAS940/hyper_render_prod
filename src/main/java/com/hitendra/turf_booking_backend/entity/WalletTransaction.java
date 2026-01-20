package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wallet transaction entity.
 * Every wallet balance change MUST have a corresponding transaction record.
 * reference_id is used for idempotency checks.
 */
@Entity
@Table(name = "wallet_transactions", indexes = {
    @Index(name = "idx_wallet_tx_wallet_id", columnList = "wallet_id"),
    @Index(name = "idx_wallet_tx_reference", columnList = "reference_id"),
    @Index(name = "idx_wallet_tx_source_ref", columnList = "source, reference_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /**
     * Transaction amount (always positive).
     * Type determines whether it's credit or debit.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WalletTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletTransactionSource source;

    /**
     * External reference for idempotency: booking_id, order_id, payment_id, etc.
     * Used to prevent duplicate transactions.
     */
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WalletTransactionStatus status = WalletTransactionStatus.PENDING;

    /**
     * Optional description/note for the transaction.
     */
    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markSuccess() {
        this.status = WalletTransactionStatus.SUCCESS;
    }

    public void markFailed() {
        this.status = WalletTransactionStatus.FAILED;
    }
}

