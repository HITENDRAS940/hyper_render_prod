package com.hitendra.turf_booking_backend.dto.booking;

import com.hitendra.turf_booking_backend.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for creating a booking with payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingPaymentRequest {

    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Total amount is required")
    private BigDecimal totalAmount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    /**
     * Amount to pay from wallet (for WALLET_ONLY or WALLET_PLUS_ONLINE).
     * If null and paymentMethod is WALLET_ONLY, uses full totalAmount.
     */
    private BigDecimal walletAmount;

    /**
     * Activity ID (optional, for filtering).
     */
    private Long activityId;
}

