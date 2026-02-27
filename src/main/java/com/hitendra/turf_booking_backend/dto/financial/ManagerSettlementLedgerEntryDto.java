package com.hitendra.turf_booking_backend.dto.financial;

import com.hitendra.turf_booking_backend.entity.SettlementPaymentMode;
import com.hitendra.turf_booking_backend.entity.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single settlement record as viewed in the manager's settlement ledger.
 * Includes admin context for cross-admin ledger views.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerSettlementLedgerEntryDto {

    private Long settlementId;

    private Long adminId;
    private String adminName;
    private String adminBusinessName;

    private BigDecimal amount;

    /** Remaining pending amount for this admin AFTER this settlement. */
    private BigDecimal pendingAfter;

    private SettlementPaymentMode paymentMode;
    private SettlementStatus status;
    private Long settledByManagerId;
    private String settlementReference;

    /** Optional notes/remarks added by the manager. */
    private String notes;

    private Instant createdAt;
}

