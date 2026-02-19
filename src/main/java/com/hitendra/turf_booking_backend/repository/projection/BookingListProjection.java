package com.hitendra.turf_booking_backend.repository.projection;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

/**
 * Lightweight projection for booking list views.
 * Fetches only essential fields without loading related entities.
 */
public interface BookingListProjection {
    Long getId();
    String getReference();
    LocalDate getBookingDate();
    LocalTime getStartTime();
    LocalTime getEndTime();
    Double getAmount();
    String getStatus();
    Instant getCreatedAt();
    BigDecimal getOnlineAmountPaid();
    BigDecimal getVenueAmountDue();
    Boolean getVenueAmountCollected();
    String getVenuePaymentCollectionMethod();
    String getPaymentStatus();

    // Service info
    Long getServiceId();
    String getServiceName();

    // Resource info
    Long getResourceId();
    String getResourceName();

    // User info (nullable for admin-created bookings)
    Long getUserId();
    String getUserName();
    String getUserEmail();
    String getUserPhone();
}
