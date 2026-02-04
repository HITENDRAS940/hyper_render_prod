package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Check if user profile exists (faster than full entity load).
     */
    boolean existsByUserId(Long userId);

    /**
     * Get user profile ID by user ID (minimal data fetch).
     */
    @Query("SELECT up.id FROM UserProfile up WHERE up.user.id = :userId")
    Optional<Long> findUserProfileIdByUserId(@Param("userId") Long userId);

    // ==================== END OPTIMIZED QUERIES ====================
}


