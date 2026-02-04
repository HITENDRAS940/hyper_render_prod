package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Optional<Activity> findByCode(String code);

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Check if activity exists by code (faster than full entity load).
     */
    boolean existsByCode(String code);

    /**
     * Get activity ID by code (minimal data fetch).
     */
    @Query("SELECT a.id FROM Activity a WHERE a.code = :code")
    Optional<Long> findActivityIdByCode(@Param("code") String code);

    /**
     * Get all enabled activities (lightweight).
     */
    @Query("SELECT a FROM Activity a WHERE a.enabled = true ORDER BY a.code")
    List<Activity> findAllEnabled();

    /**
     * Get only activity codes (for validation without entity load).
     */
    @Query("SELECT a.code FROM Activity a WHERE a.enabled = true")
    List<String> findAllEnabledActivityCodes();

    // ==================== END OPTIMIZED QUERIES ====================
}

