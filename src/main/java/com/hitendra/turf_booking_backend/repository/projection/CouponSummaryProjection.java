package com.hitendra.turf_booking_backend.repository.projection;

import java.time.LocalDate;

/**
 * Lightweight projection for coupon list views.
 * Fetches ONLY id, code, description, and expiryDate — nothing else is loaded from the DB.
 */
public interface CouponSummaryProjection {
    Long getId();
    String getCode();
    String getDescription();
    LocalDate getExpiryDate();
}
