package com.hitendra.turf_booking_backend.dto.wallet;

import com.hitendra.turf_booking_backend.entity.WalletStatus;
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
public class WalletDto {
    private Long id;
    private Long userId;
    private BigDecimal balance;
    private WalletStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}

