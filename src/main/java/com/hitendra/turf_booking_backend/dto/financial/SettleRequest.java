package com.hitendra.turf_booking_backend.dto.financial;

import com.hitendra.turf_booking_backend.entity.SettlementPaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request body for POST /manager/admin/{id}/settle
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettleRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Payment mode is required")
    private SettlementPaymentMode paymentMode;

    private String settlementReference;

    /** Optional notes or remarks for this settlement. */
    private String notes;
}

