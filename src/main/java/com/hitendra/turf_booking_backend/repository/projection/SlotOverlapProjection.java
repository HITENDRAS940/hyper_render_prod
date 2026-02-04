package com.hitendra.turf_booking_backend.repository.projection;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Minimal projection for slot availability checking.
 * Only includes fields needed for overlap detection.
 */
public interface SlotOverlapProjection {
    Long getId();
    Long getResourceId();
    LocalDate getBookingDate();
    LocalTime getStartTime();
    LocalTime getEndTime();
    String getStatus();
    String getPaymentStatus();
}
