package com.hitendra.turf_booking_backend.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for manual UPI booking with soft-locking information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualUpiBookingResponseDto {

    private Long bookingId;
    private String reference;
    private String status;

    private Long serviceId;
    private String serviceName;

    private String activityCode;
    private LocalDate bookingDate;
    private String startTime;
    private String endTime;
    private String slotTime;

    private Double amount;
    private AmountBreakdown amountBreakdown;

    private String paymentMode;
    private Instant lockExpiresAt;
    private Long lockExpiresInSeconds;

    private boolean isResume;  // true if this is a resumed existing booking

    private Instant createdAt;

    /**
     * Amount breakdown details
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AmountBreakdown {
        private Double slotSubtotal;
        private Double platformFeePercent;
        private Double platformFee;
        private Double totalAmount;
        private String currency;
    }
}

