package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminProfileRepository extends JpaRepository<AdminProfile, Long> {
    Optional<AdminProfile> findByUserId(Long userId);

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Get admin profile ID by user ID (minimal data fetch).
     */
    @Query("SELECT a.id FROM AdminProfile a WHERE a.user.id = :userId")
    Optional<Long> findAdminProfileIdByUserId(@Param("userId") Long userId);

    /**
     * Check if admin profile exists for user (faster than full entity load).
     */
    boolean existsByUserId(Long userId);

    // ==================== END OPTIMIZED QUERIES ====================
}

