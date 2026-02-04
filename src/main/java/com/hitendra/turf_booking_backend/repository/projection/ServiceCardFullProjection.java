package com.hitendra.turf_booking_backend.repository.projection;

import java.util.List;

/**
 * Complete projection for service card views.
 * Includes all fields needed for UI display: id, name, location, availability, images, description.
 */
public interface ServiceCardFullProjection {
    Long getId();
    String getName();
    String getLocation();
    boolean getAvailability();
    List<String> getImages();
    String getDescription();
}
