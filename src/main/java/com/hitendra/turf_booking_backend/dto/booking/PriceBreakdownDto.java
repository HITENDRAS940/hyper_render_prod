package com.hitendra.turf_booking_backend.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for detailed price breakdown including taxes and fees
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceBreakdownDto {

    // Base slot prices
    private Double slotSubtotal;

    // Extra charges from price rules
    private Double extraCharges;

    // List of applied price rules with reasons
    private List<AppliedRule> appliedRules;

    // Subtotal before taxes (slotSubtotal + extraCharges)
    private Double subtotal;

    // Tax details
//    private Double taxRate;           // e.g., 18.0 for 18%
//    private Double taxAmount;

    // Convenience/Platform fee
    private Double convenienceFee;
    private Double convenienceFeeRate; // e.g., 2.0 for 2%

    // Discount if any
    private Double discountAmount;
    private String discountCode;
    private String discountReason;

    // Final amount to pay
    private Double totalAmount;

    // Currency
    @Builder.Default
    private String currency = "INR";

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppliedRule {
        private Long ruleId;
        private String reason;
        private Double extraCharge;
        private String timeRange;      // e.g., "18:00 - 23:00"
        private String dayType;        // WEEKDAY, WEEKEND, ALL
    }
}

