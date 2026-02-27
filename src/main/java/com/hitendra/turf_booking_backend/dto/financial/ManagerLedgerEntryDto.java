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
 * A single ledger entry for the manager's unified view (cash + bank combined).
 * Includes which admin the entry belongs to.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerLedgerEntryDto {

    private Long id;

    /** Admin this ledger entry belongs to. */
    private Long adminId;
    private String adminName;

    /** CASH or BANK sub-ledger. */
    private AdminLedgerType ledgerType;

    /** CREDIT (money in) or DEBIT (money out). */
    private AdminLedgerEntryType entryType;

    private BigDecimal amount;

    /** Running balance of the admin's sub-ledger AFTER this entry. */
    private BigDecimal balanceAfter;

    private String description;

    /** BOOKING, SETTLEMENT, EXPENSE, ADJUSTMENT, etc. */
    private String referenceType;

    private Long referenceId;

    private Instant createdAt;
}

