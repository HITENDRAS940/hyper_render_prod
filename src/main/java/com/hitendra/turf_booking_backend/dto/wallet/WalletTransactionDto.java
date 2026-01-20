package com.hitendra.turf_booking_backend.dto.wallet;

import com.hitendra.turf_booking_backend.entity.WalletTransactionSource;
import com.hitendra.turf_booking_backend.entity.WalletTransactionStatus;
import com.hitendra.turf_booking_backend.entity.WalletTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionDto {
    private Long id;
    private Long walletId;
    private BigDecimal amount;
    private WalletTransactionType type;
    private WalletTransactionSource source;
    private String referenceId;
    private WalletTransactionStatus status;
    private String description;
    private Instant createdAt;
}

