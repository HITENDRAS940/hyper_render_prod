package com.hitendra.turf_booking_backend.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderRequest {
    private Long bookingId;
    private Double amount;
    private String currency;
    private String receipt;
}

