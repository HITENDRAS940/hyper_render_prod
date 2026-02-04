package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.ResourceSlotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResourceSlotConfigRepository extends JpaRepository<ResourceSlotConfig, Long> {

    Optional<ResourceSlotConfig> findByResourceId(Long resourceId);

    Optional<ResourceSlotConfig> findByResourceIdAndEnabledTrue(Long resourceId);

    List<ResourceSlotConfig> findByResourceIdIn(java.util.List<Long> resourceIds);

    void deleteByResourceId(Long resourceId);

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Check if slot config exists for resource (faster than full entity load).
     */
    boolean existsByResourceId(Long resourceId);

    /**
     * Check if enabled slot config exists for resource.
     */
    @Query("SELECT CASE WHEN COUNT(sc) > 0 THEN true ELSE false END FROM ResourceSlotConfig sc WHERE sc.resource.id = :resourceId AND sc.enabled = true")
    boolean existsEnabledByResourceId(@Param("resourceId") Long resourceId);

    /**
     * Get only the slot duration for a resource (minimal data for slot generation).
     */
    @Query("SELECT sc.slotDurationMinutes FROM ResourceSlotConfig sc WHERE sc.resource.id = :resourceId AND sc.enabled = true")
    Optional<Integer> findSlotDurationByResourceId(@Param("resourceId") Long resourceId);

    /**
     * Get slot config ID by resource ID (minimal data fetch).
     */
    @Query("SELECT sc.id FROM ResourceSlotConfig sc WHERE sc.resource.id = :resourceId")
    Optional<Long> findSlotConfigIdByResourceId(@Param("resourceId") Long resourceId);

    /**
     * Get resource IDs that have enabled slot configs (for batch operations).
     */
    @Query("SELECT sc.resource.id FROM ResourceSlotConfig sc WHERE sc.resource.id IN :resourceIds AND sc.enabled = true")
    List<Long> findResourceIdsWithEnabledConfig(@Param("resourceIds") List<Long> resourceIds);

    // ==================== END OPTIMIZED QUERIES ====================
}
