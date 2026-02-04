package com.hitendra.turf_booking_backend.repository.projection;

/**
 * Minimal projection for booking count statistics.
 */
public interface BookingCountProjection {
    String getStatus();
    Long getCount();
}
