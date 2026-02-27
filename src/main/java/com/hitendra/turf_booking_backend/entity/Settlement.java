package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a settlement where the manager transfers platform-collected
 * online advance amounts to the admin's bank account.
 *
 * FLOW:
 *   pendingOnlineAmount → admin.bankBalance
 *   admin.totalSettledAmount += amount
 */
@Entity
@Table(name = "settlements", indexes = {
    @Index(name = "idx_settlement_admin", columnList = "admin_id"),
    @Index(name = "idx_settlement_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Admin receiving the settlement. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private AdminProfile admin;

    /** Amount being settled. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Transfer mode used by the manager. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
    private SettlementPaymentMode paymentMode;

    /** Current status of the settlement. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.INITIATED;

    /** Manager who initiated the settlement. */
    @Column(name = "settled_by_manager_id")
    private Long settledByManagerId;

    /** External reference (UTR, transaction ID, etc.). */
    @Column(name = "settlement_reference")
    private String settlementReference;

    /** Optional notes or remarks added by the manager for this settlement. */
    @Column(name = "notes", length = 1000)
    private String notes;

    /** Admin's pending online amount AFTER this settlement was applied (snapshot). */
    @Column(name = "pending_after_settlement", precision = 19, scale = 2)
    private BigDecimal pendingAfterSettlement;

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

