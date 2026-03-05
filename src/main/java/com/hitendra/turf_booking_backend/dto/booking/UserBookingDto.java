package com.hitendra.turf_booking_backend.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBookingDto {
    private Long id;
    private String reference;                  // Booking reference ID
    private Long serviceId;
    private String serviceName;
    private Long resourceId;
    private String resourceName;
    private String status;
    private LocalDate date;
    private List<SlotTimeDto> slots;
    private AmountBreakdown amountBreakdown;
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotTimeDto {
        private String startTime;
        private String endTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AmountBreakdown {
        private Double originalAmount;      // Total before any discount
        private Double discountAmount;      // Discount applied via coupon (0 if none)
        private String couponCode;          // Coupon code used (null if none)
        private Double totalAmount;         // Final amount after discount
        private Double paidAmount;          // Amount already paid online
        private Double dueAmount;           // Amount due at venue
    }
}
