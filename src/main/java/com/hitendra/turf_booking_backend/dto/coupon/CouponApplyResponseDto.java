package com.hitendra.turf_booking_backend.dto.coupon;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response returned after successfully applying a coupon to a booking.
 * Contains only the revised amount breakdown — no other booking details.
 * All values are already persisted to the bookings table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CouponApplyResponseDto {

    private Long bookingId;
    private String couponCode;

    private Double originalAmount;      // Total before discount
    private Double discountAmount;      // Amount saved by the coupon
    private Double revisedTotal;        // New total after discount

    private Double onlineAmount;        // Amount to pay online  (already saved)
    private Double venueAmount;         // Amount to pay at venue (already saved)
    private Double onlinePaymentPercent;// Percentage split used

    private String currency;
}

