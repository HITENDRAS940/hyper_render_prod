package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Coupon;
import com.hitendra.turf_booking_backend.repository.projection.CouponSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeAndActiveTrue(String code);

    boolean existsByCode(String code);

    /** Admin view — all coupons, id + code + description only. */
    @Query("SELECT c.id AS id, c.code AS code, c.description AS description FROM Coupon c ORDER BY c.id ASC")
    List<CouponSummaryProjection> findAllSummaries();

    /**
     * Public view — only active coupons whose validity window is open right now.
     * Fetches id, code, description — nothing else.
     */
    @Query("""
            SELECT c.id AS id, c.code AS code, c.description AS description
            FROM Coupon c
            WHERE c.active = true
              AND c.expiryDate >= :today
              AND (c.validFrom IS NULL OR c.validFrom <= :today)
            ORDER BY c.id ASC
            """)
    List<CouponSummaryProjection> findAllAvailableSummaries(@Param("today") java.time.LocalDate today);
}
