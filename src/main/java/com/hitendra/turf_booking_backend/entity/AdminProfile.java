package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "admin_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column
    private String city;

    @Column
    private String businessName;

    @Column
    private String businessAddress;

    @Column
    private String gstNumber;

    // ─── Financial Tracking ───────────────────────────────────────────────────

    /**
     * Total cash collected directly at the venue by this admin.
     * Increases when remaining payment is paid in CASH at the venue.
     */
    @Column(name = "total_cash_collected", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalCashCollected = BigDecimal.ZERO;

    /**
     * Total amount collected via online/UPI directly to admin's bank at venue.
     * Increases when remaining payment is paid ONLINE directly to admin.
     */
    @Column(name = "total_bank_collected", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalBankCollected = BigDecimal.ZERO;

    /**
     * Total online advance amount collected by the platform on behalf of this admin.
     * Increases when a Razorpay payment is captured.
     */
    @Column(name = "total_platform_online_collected", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalPlatformOnlineCollected = BigDecimal.ZERO;

    /**
     * Total amount already settled (transferred) by manager to admin.
     * Increases on each successful settlement.
     */
    @Column(name = "total_settled_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalSettledAmount = BigDecimal.ZERO;

    /**
     * Platform-collected online advance amounts not yet settled to admin.
     * Increases on Razorpay capture; decreases on settlement.
     */
    @Column(name = "pending_online_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal pendingOnlineAmount = BigDecimal.ZERO;

    /**
     * Running cash balance (from venue cash collections).
     * Increases on VENUE_CASH payments.
     */
    @Column(name = "cash_balance", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal cashBalance = BigDecimal.ZERO;

    /**
     * Running bank/UPI balance (venue direct + settled amounts).
     * Increases on VENUE_BANK payments and on settlements.
     */
    @Column(name = "bank_balance", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal bankBalance = BigDecimal.ZERO;

    /**
     * Computed total balance = cashBalance + bankBalance.
     * NOT stored in DB.
     */
    @Transient
    public BigDecimal getCurrentBalance() {
        BigDecimal cash = cashBalance != null ? cashBalance : BigDecimal.ZERO;
        BigDecimal bank = bankBalance != null ? bankBalance : BigDecimal.ZERO;
        return cash.add(bank);
    }

    // ─── Relationships ────────────────────────────────────────────────────────

    // Services managed by this admin
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Service> managedServices = new ArrayList<>();
}

