package com.hitendra.turf_booking_backend.repository.projection;

/**
 * Minimal projection for user list views.
 * Only includes essential user information.
 */
public interface UserBasicProjection {
    Long getId();
    String getPhone();
    String getEmail();
    String getName();
    String getRole();
    boolean getEnabled();
    java.time.Instant getCreatedAt();
}
