package com.hitendra.turf_booking_backend.repository.projection;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Minimal projection for user's booking history.
 * Only includes fields needed for the user booking list view.
 */
public interface UserBookingProjection {
    Long getId();
    String getStatus();
    LocalDate getBookingDate();
    LocalTime getStartTime();
    LocalTime getEndTime();
    Double getAmount();
    Instant getCreatedAt();

    // Service info
    Long getServiceId();
    String getServiceName();

    // Resource info
    Long getResourceId();
    String getResourceName();
}
