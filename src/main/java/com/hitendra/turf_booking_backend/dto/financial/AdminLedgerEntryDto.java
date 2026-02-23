package com.hitendra.turf_booking_backend.dto.financial;

import com.hitendra.turf_booking_backend.entity.AdminLedgerEntryType;
import com.hitendra.turf_booking_backend.entity.AdminLedgerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Single line item in the admin's cash or bank ledger statement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLedgerEntryDto {

    private Long id;

    /** CASH or BANK */
    private AdminLedgerType ledgerType;

    /** CREDIT (money in) or DEBIT (money out) */
    private AdminLedgerEntryType entryType;

    private BigDecimal amount;

    /** Running balance after this entry */
    private BigDecimal balanceAfter;

    private String description;

    /** BOOKING, SETTLEMENT, EXPENSE, ADJUSTMENT */
    private String referenceType;

    private Long referenceId;

    private Instant createdAt;
}

