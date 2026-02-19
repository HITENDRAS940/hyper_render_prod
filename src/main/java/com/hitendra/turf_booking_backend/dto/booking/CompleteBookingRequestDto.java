package com.hitendra.turf_booking_backend.dto.booking;

import com.hitendra.turf_booking_backend.entity.VenuePaymentCollectionMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for completing a booking with venue payment collection details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteBookingRequestDto {

    /**
     * Amount collected at venue.
     */
    @NotNull(message = "Amount collected is required")
    @Positive(message = "Amount collected must be positive")
    private BigDecimal amountCollected;

    /**
     * Method of collection: CASH or ONLINE.
     */
    @NotNull(message = "Collection method is required")
    private VenuePaymentCollectionMethod collectionMethod;
}

