package com.hitendra.turf_booking_backend.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for wallet top-up initiation.
 * Contains transaction reference and amount for payment processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTopupResponse {
    private String reference;  // Transaction reference for payment gateway
    private BigDecimal amount;
    private String status;     // Transaction status (PENDING initially)
}

