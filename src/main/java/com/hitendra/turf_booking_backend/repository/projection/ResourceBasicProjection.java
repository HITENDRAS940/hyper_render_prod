package com.hitendra.turf_booking_backend.repository.projection;

/**
 * Minimal projection for resource list views.
 * Only includes essential resource information.
 */
public interface ResourceBasicProjection {
    Long getId();
    String getName();
    String getDescription();
    boolean getEnabled();
    Long getServiceId();
}
