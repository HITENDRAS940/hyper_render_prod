package com.hitendra.turf_booking_backend.dto.financial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Full financial snapshot for a single admin.
 * Returned to both Manager and Admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFinancialOverviewDto {

    private Long adminId;
    private String adminName;

    /** Cash collected directly at venue. */
    private BigDecimal cashBalance;

    /** Bank/UPI balance (direct venue online + settled amounts). */
    private BigDecimal bankBalance;

    /** Computed: cashBalance + bankBalance */
    private BigDecimal currentBalance;

    /** Platform-collected advance not yet settled to admin. */
    private BigDecimal pendingOnlineAmount;

    /** Lifetime cash collected at venue. */
    private BigDecimal totalCashCollected;

    /** Lifetime bank/UPI collected at venue (direct to admin). */
    private BigDecimal totalBankCollected;

    /** Lifetime online advances collected by platform for this admin. */
    private BigDecimal totalPlatformOnlineCollected;

    /** Lifetime amount settled by manager to admin. */
    private BigDecimal totalSettledAmount;
}

