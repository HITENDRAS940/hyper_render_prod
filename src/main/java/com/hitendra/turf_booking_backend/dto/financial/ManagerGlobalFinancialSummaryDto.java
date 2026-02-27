package com.hitendra.turf_booking_backend.dto.financial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Aggregate financial snapshot for the manager's global dashboard.
 * Sums up money across ALL admins.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerGlobalFinancialSummaryDto {

    /** Total admins with a non-zero pending online amount. */
    private long adminsWithPendingBalance;

    /** Sum of all admins' pendingOnlineAmount — total platform owes admins. */
    private BigDecimal totalPendingOnlineAmount;

    /** Sum of all admins' totalSettledAmount — total ever settled. */
    private BigDecimal totalSettledAmount;

    /** Sum of all admins' cashBalance — total cash in hands of all admins. */
    private BigDecimal totalCashBalance;

    /** Sum of all admins' bankBalance — total bank balance across all admins. */
    private BigDecimal totalBankBalance;

    /** Sum of totalPlatformOnlineCollected across all admins. */
    private BigDecimal totalPlatformOnlineCollected;

    /** Sum of totalCashCollected across all admins. */
    private BigDecimal totalCashCollected;

    /** Sum of totalBankCollected (direct venue bank) across all admins. */
    private BigDecimal totalBankCollected;
}

