package com.hitendra.turf_booking_backend.dto.financial;

import com.hitendra.turf_booking_backend.entity.FinancialTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for a single financial transaction audit record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialTransactionDto {

    private Long id;
    private FinancialTransactionType type;
    private BigDecimal amount;
    private Long referenceId;
    private Instant createdAt;
}

