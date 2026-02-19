package com.hitendra.turf_booking_backend.dto.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.PaymentMode;
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
    private Long serviceId;
    private String serviceName;
    private String adjustmentType; // CREDIT or DEBIT
    private Double amount;
    private PaymentMode paymentMode;
    private String description;
    private String referenceNumber;
    private Double previousBalance;
    private Double newBalance;
    private String recordedBy;
    private Instant recordedAt;
}

