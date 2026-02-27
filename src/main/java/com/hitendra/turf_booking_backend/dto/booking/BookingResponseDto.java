package com.hitendra.turf_booking_backend.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {
    private Long id;
    private String reference;
    private Long serviceId;
    private String serviceName;
    private Long resourceId;
    private String resourceName;
    private String startTime;
    private String endTime;
    private LocalDate bookingDate;
    private Instant createdAt;

    // Price breakdown details
    private AmountBreakdown amountBreakdown;

    /** Pricing model: PER_SLOT or PER_PERSON */
    private String pricingType;

    /** Number of persons included in this booking (≥1; relevant for PER_PERSON pricing) */
    private Integer numberOfPersons;

    private String bookingType; // SINGLE_RESOURCE or MULTI_RESOURCE
    private String message;     // For PARTIAL_AVAILABLE or error messages
    private java.util.List<BookingResponseDto> childBookings; // For split bookings
    private String status;      // SUCCESS, PARTIAL_AVAILABLE, FAILED, PENDING, CONFIRMED, CANCELLED
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AmountBreakdown {
        private Double pricePerPerson;     // Base price per person / per slot (before person multiplier)
        private Integer numberOfPersons;   // Number of persons (1 for PER_SLOT bookings)
        private Double slotSubtotal;       // Base price of slots × numberOfPersons (without platform fee)
        private Double platformFeePercent; // Platform fee percentage (2%)
        private Double platformFee;        // Platform fee amount (2% of subtotal)
        private Double totalAmount;        // Final amount to pay (slotSubtotal + platformFee)
        private Double onlinePaymentPercent; // Percentage of total to pay online
        private Double onlineAmount;       // Amount paid online (X% of totalAmount)
        private Double venueAmount;        // Amount to pay at venue (100-X% of totalAmount)
        private Boolean venueAmountCollected; // Whether venue amount has been collected
        private String venuePaymentCollectionMethod; // Method used to collect venue amount (CASH or ONLINE)
        private String currency;           // Currency code (e.g., "INR")
    }
}
