package com.hitendra.turf_booking_backend.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for displaying complete user information (excluding admins)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfoDto {
    private Long id;
    private String phone;
    private String email;
    private String name;
    private String role;
    private boolean enabled;
    private Instant createdAt;

    // Wallet information
    private WalletInfo wallet;

    // Booking statistics
    private Long totalBookings;
    private Long confirmedBookings;
    private Long cancelledBookings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletInfo {
        private Long walletId;
        private BigDecimal balance;
        private String status;
    }
}

