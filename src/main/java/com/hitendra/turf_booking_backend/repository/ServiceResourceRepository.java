package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.ServiceResource;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
     * Find resource by ID with pessimistic lock for booking.
     * Prevents concurrent booking on the same resource.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sr FROM ServiceResource sr WHERE sr.id = :id AND sr.enabled = true")
    Optional<ServiceResource> findByIdWithLock(@Param("id") Long id);

    /**
     * Find all enabled resources for a service that support a specific activity,
     * with pessimistic lock for concurrent booking prevention.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
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
}

