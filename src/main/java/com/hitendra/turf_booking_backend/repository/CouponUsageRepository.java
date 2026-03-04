package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    /** How many times this user has already used this coupon. */
    long countByUserIdAndCouponId(Long userId, Long couponId);

    /** Total redemptions of a coupon across all users. */
    long countByCouponId(Long couponId);

    /** Whether a usage record already exists for the given booking (idempotency guard). */
    boolean existsByBookingId(Long bookingId);
}
