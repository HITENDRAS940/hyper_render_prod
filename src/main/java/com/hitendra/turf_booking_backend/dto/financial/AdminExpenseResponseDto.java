package com.hitendra.turf_booking_backend.dto.financial;

import com.hitendra.turf_booking_backend.entity.AdminLedgerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response for a recorded admin-level expense.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminExpenseResponseDto {

    private Long ledgerEntryId;

    /** Which sub-ledger was debited */
    private AdminLedgerType ledgerType;

    private BigDecimal amount;

    /** Balance of that sub-ledger after this expense */
    private BigDecimal balanceAfter;

    private String description;
    private String category;

    private Instant createdAt;
}

