package com.hitendra.turf_booking_backend.dto.accounting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for manual adjustment operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualAdjustmentResponseDto {

    private Long ledgerId;
    private Long adminId;
    private String adjustmentType;  // CREDIT or DEBIT
    private String paymentMode;     // CASH or BANK
    private Double amount;
    private String description;
    private String referenceNumber;
    private Double previousBalance;
    private Double newBalance;
    private String recordedBy;
    private Instant recordedAt;
}
