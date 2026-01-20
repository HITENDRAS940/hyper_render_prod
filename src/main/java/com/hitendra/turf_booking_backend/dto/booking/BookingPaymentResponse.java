package com.hitendra.turf_booking_backend.dto.booking;

import com.hitendra.turf_booking_backend.entity.BookingStatus;
import com.hitendra.turf_booking_backend.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for booking creation with payment details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingPaymentResponse {

    private Long bookingId;
    private String bookingReference;
    private BookingStatus status;
    private PaymentMethod paymentMethod;

    // Amount breakdown
    private BigDecimal totalAmount;
    private BigDecimal walletAmountUsed;
    private BigDecimal onlineAmountDue;

    // Payment order details (for online payment)
    private String paymentOrderId;
    private String paymentSessionId;

    // Wallet transaction ID (if wallet was used)
    private Long walletTransactionId;

    private String message;
}

