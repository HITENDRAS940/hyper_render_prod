package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.ServiceResource;
import com.hitendra.turf_booking_backend.repository.projection.ResourceBasicProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceResourceRepository extends JpaRepository<ServiceResource, Long> {

    List<ServiceResource> findByServiceId(Long serviceId);

    List<ServiceResource> findByServiceIdAndEnabledTrue(Long serviceId);

    boolean existsByServiceIdAndName(Long serviceId, String name);

    /**
     * Find all enabled resources for a service that support a specific activity.
     * Used for pooling identical resources.
     */
    @Query("SELECT DISTINCT sr FROM ServiceResource sr " +
           "JOIN sr.activities a " +
           "WHERE sr.service.id = :serviceId " +
           "AND a.code = :activityCode " +
           "AND sr.enabled = true " +
           "AND a.enabled = true")
    List<ServiceResource> findByServiceIdAndActivityCode(
            @Param("serviceId") Long serviceId,
            @Param("activityCode") String activityCode);

    /**
     * Find resource by ID (enabled only).
     */
    @Query("SELECT sr FROM ServiceResource sr WHERE sr.id = :id AND sr.enabled = true")
    Optional<ServiceResource> findByIdWithLock(@Param("id") Long id);

    /**
     * Find all enabled resources for a service that support a specific activity.
     * NOTE: Pessimistic lock removed. Concurrent booking safety is handled by
     * READ_COMMITTED isolation + partial unique index on bookings table.
     */
    @Query("SELECT DISTINCT sr FROM ServiceResource sr " +
           "JOIN sr.activities a " +
           "WHERE sr.service.id = :serviceId " +
           "AND a.code = :activityCode " +
           "AND sr.enabled = true " +
           "AND a.enabled = true " +
           "ORDER BY sr.id")
    List<ServiceResource> findByServiceIdAndActivityCodeWithLock(
            @Param("serviceId") Long serviceId,
            @Param("activityCode") String activityCode);

    /**
     * Count enabled resources for a service that support a specific activity.
     * Used to show total pool size.
     */
    @Query("SELECT COUNT(DISTINCT sr) FROM ServiceResource sr " +
           "JOIN sr.activities a " +
           "WHERE sr.service.id = :serviceId " +
           "AND a.code = :activityCode " +
           "AND sr.enabled = true " +
           "AND a.enabled = true")
    int countByServiceIdAndActivityCode(
            @Param("serviceId") Long serviceId,
            @Param("activityCode") String activityCode);

    // ==================== OPTIMIZED PROJECTION QUERIES ====================

    /**
     * Get lightweight resource list for a service (projection-based).
     */
    @Query("""
        SELECT sr.id as id, sr.name as name, sr.description as description, 
               sr.enabled as enabled, sr.service.id as serviceId
        FROM ServiceResource sr
        WHERE sr.service.id = :serviceId
        ORDER BY sr.id
        """)
    List<ResourceBasicProjection> findResourcesByServiceIdProjected(@Param("serviceId") Long serviceId);

    /**
     * Get lightweight enabled resource list for a service (projection-based).
     */
    @Query("""
        SELECT sr.id as id, sr.name as name, sr.description as description, 
               sr.enabled as enabled, sr.service.id as serviceId
        FROM ServiceResource sr
        WHERE sr.service.id = :serviceId AND sr.enabled = true
        ORDER BY sr.id
        """)
    List<ResourceBasicProjection> findEnabledResourcesByServiceIdProjected(@Param("serviceId") Long serviceId);

    /**
     * Get only resource IDs for a service (for batch operations).
     */
    @Query("SELECT sr.id FROM ServiceResource sr WHERE sr.service.id = :serviceId AND sr.enabled = true")
    List<Long> findEnabledResourceIdsByServiceId(@Param("serviceId") Long serviceId);

    /**
     * Get only resource IDs for a service and activity (for batch operations).
     */
    @Query("SELECT DISTINCT sr.id FROM ServiceResource sr " +
           "JOIN sr.activities a " +
           "WHERE sr.service.id = :serviceId " +
           "AND a.code = :activityCode " +
           "AND sr.enabled = true " +
           "AND a.enabled = true")
    List<Long> findResourceIdsByServiceIdAndActivityCode(
            @Param("serviceId") Long serviceId,
            @Param("activityCode") String activityCode);

    /**
     * Get resource name by ID (minimal data fetch).
     */
    @Query("SELECT sr.name FROM ServiceResource sr WHERE sr.id = :resourceId")
    Optional<String> findResourceNameById(@Param("resourceId") Long resourceId);

    /**
     * Check if resource exists and is enabled (faster than full entity load).
     */
    @Query("SELECT CASE WHEN COUNT(sr) > 0 THEN true ELSE false END FROM ServiceResource sr WHERE sr.id = :resourceId AND sr.enabled = true")
    boolean existsByIdAndEnabled(@Param("resourceId") Long resourceId);

    /**
     * Get service ID for a resource (minimal data fetch).
     */
    @Query("SELECT sr.service.id FROM ServiceResource sr WHERE sr.id = :resourceId")
    Optional<Long> findServiceIdByResourceId(@Param("resourceId") Long resourceId);

    // ==================== END OPTIMIZED PROJECTION QUERIES ====================
}

