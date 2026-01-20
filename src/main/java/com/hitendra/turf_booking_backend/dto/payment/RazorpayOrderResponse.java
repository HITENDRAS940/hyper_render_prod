package com.hitendra.turf_booking_backend.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponse {
    private String orderId;
    private String amount;
    private String currency;
    private String receipt;
    private String status;
    private Long bookingId;
    private String keyId; // For frontend to initialize Razorpay
}

