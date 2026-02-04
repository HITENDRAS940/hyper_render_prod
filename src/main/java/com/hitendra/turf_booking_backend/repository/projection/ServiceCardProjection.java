package com.hitendra.turf_booking_backend.repository.projection;

/**
 * Minimal projection for service card views.
 * Only includes fields needed for service list/search.
 */
public interface ServiceCardProjection {
    Long getId();
    String getName();
    String getLocation();
    String getCity();
    boolean getAvailability();
}
