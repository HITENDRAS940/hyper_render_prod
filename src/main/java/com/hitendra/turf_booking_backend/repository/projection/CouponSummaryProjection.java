package com.hitendra.turf_booking_backend.repository.projection;

/**
 * Lightweight projection for coupon list views.
 * Fetches ONLY id, code, and description — nothing else is loaded from the DB.
 */
public interface CouponSummaryProjection {
    Long getId();
    String getCode();
    String getDescription();
}
