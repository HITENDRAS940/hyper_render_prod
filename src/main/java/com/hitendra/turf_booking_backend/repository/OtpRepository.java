package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByPhoneAndUsedFalseOrderByExpiresAtDesc(String phone);

    Optional<Otp> findTopByEmailAndUsedFalseOrderByExpiresAtDesc(String email);

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Check if valid (not used, not expired) OTP exists for phone (faster validation).
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Otp o WHERE o.phone = :phone AND o.used = false AND o.expiresAt > :now")
    boolean existsValidOtpByPhone(@Param("phone") String phone, @Param("now") Instant now);

    /**
     * Check if valid (not used, not expired) OTP exists for email (faster validation).
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Otp o WHERE o.email = :email AND o.used = false AND o.expiresAt > :now")
    boolean existsValidOtpByEmail(@Param("email") String email, @Param("now") Instant now);

    /**
     * Mark OTP as used by ID (direct update without loading entity).
     */
    @Modifying
    @Query("UPDATE Otp o SET o.used = true WHERE o.id = :id")
    int markOtpAsUsed(@Param("id") Long id);

    /**
     * Delete expired OTPs (cleanup job).
     */
    @Modifying
    @Query("DELETE FROM Otp o WHERE o.expiresAt < :now")
    int deleteExpiredOtps(@Param("now") Instant now);

    // ==================== END OPTIMIZED QUERIES ====================
}

