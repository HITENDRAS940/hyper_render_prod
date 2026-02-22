package com.hitendra.turf_booking_backend.dto.financial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Summary of how much the platform owes each admin.
 * Returned by GET /manager/admins/due-summary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDueSummaryDto {

    private Long adminId;
    private String adminName;

    /** Amount platform still owes this admin (not yet settled). */
    private BigDecimal pendingOnlineAmount;

    /** Total amount already settled to this admin. */
    private BigDecimal totalSettled;
}

