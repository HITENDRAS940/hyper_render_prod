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
 * Response DTO for a settlement record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDto {

    private Long id;
    private Long adminId;
    private String adminName;
    private BigDecimal amount;
    private SettlementPaymentMode paymentMode;
    private SettlementStatus status;
    private Long settledByManagerId;
    private String settlementReference;
    private Instant createdAt;
}

