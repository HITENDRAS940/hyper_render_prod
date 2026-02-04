package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.DisabledSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DisabledSlotRepository extends JpaRepository<DisabledSlot, Long> {

    /**
     * Check if a specific slot is disabled for a resource on a specific date
     */
    Optional<DisabledSlot> findByResourceIdAndStartTimeAndDisabledDate(Long resourceId, LocalTime startTime, LocalDate disabledDate);

    /**
     * Check if a slot is disabled
     */
    boolean existsByResourceIdAndStartTimeAndDisabledDate(Long resourceId, LocalTime startTime, LocalDate disabledDate);

    /**
     * Get all disabled slots for a resource on a specific date
     */
    List<DisabledSlot> findByResourceIdAndDisabledDate(Long resourceId, LocalDate disabledDate);

    /**
     * Get all disabled slots for a service on a specific date
     */
    List<DisabledSlot> findByResourceServiceIdAndDisabledDate(Long serviceId, LocalDate disabledDate);

    /**
     * Get all disabled slots for a service
     */
    List<DisabledSlot> findByResourceServiceId(Long serviceId);

    /**
     * Get all disabled slots for a service within a date range
     */
    @Query("SELECT ds FROM DisabledSlot ds WHERE ds.resource.service.id = :serviceId AND ds.disabledDate BETWEEN :startDate AND :endDate")
    List<DisabledSlot> findByServiceIdAndDateRange(
            @Param("serviceId") Long serviceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find IDs of services that have disabled slots overlapping the given time range.
     * Used for fast availability filtering.
     */
    @Query("SELECT DISTINCT ds.resource.service.id FROM DisabledSlot ds " +
           "JOIN ds.resource.activities a " +
           "WHERE ds.disabledDate = :date AND " +
           "(:activityCode IS NULL OR a.code = :activityCode) AND " +
           "ds.startTime < :endTime AND ds.endTime > :startTime")
    List<Long> findBusyServiceIds(
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("activityCode") String activityCode
    );

    /**
     * Delete a specific disabled slot
     */
    void deleteByResourceIdAndStartTimeAndDisabledDate(Long resourceId, LocalTime startTime, LocalDate disabledDate);

    /**
     * Delete all disabled slots for a resource on a specific date
     */
    void deleteByResourceIdAndDisabledDate(Long resourceId, LocalDate disabledDate);

    /**
     * Find overlapping disabled slots for a list of resources.
     */
    @Query("SELECT ds FROM DisabledSlot ds WHERE ds.resource.id IN :resourceIds AND ds.disabledDate = :date AND ds.startTime < :endTime AND ds.endTime > :startTime")
    List<DisabledSlot> findOverlappingDisabledSlots(
            @Param("resourceIds") List<Long> resourceIds,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * Get all disabled slots for a resource
     */
    List<DisabledSlot> findByResourceId(Long resourceId);

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Check if any overlapping disabled slot exists (returns boolean for fast check).
     */
    @Query("""
        SELECT CASE WHEN COUNT(ds) > 0 THEN true ELSE false END
        FROM DisabledSlot ds 
        WHERE ds.resource.id = :resourceId 
        AND ds.disabledDate = :date 
        AND ds.startTime < :endTime AND ds.endTime > :startTime
        """)
    boolean existsOverlappingDisabledSlot(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * Check if any overlapping disabled slot exists for multiple resources (returns boolean).
     */
    @Query("""
        SELECT CASE WHEN COUNT(ds) > 0 THEN true ELSE false END
        FROM DisabledSlot ds 
        WHERE ds.resource.id IN :resourceIds 
        AND ds.disabledDate = :date 
        AND ds.startTime < :endTime AND ds.endTime > :startTime
        """)
    boolean existsOverlappingDisabledSlotForResources(
            @Param("resourceIds") List<Long> resourceIds,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * Get only disabled slot times for a resource on a date (minimal data).
     */
    @Query("SELECT ds.startTime, ds.endTime FROM DisabledSlot ds WHERE ds.resource.id = :resourceId AND ds.disabledDate = :date ORDER BY ds.startTime")
    List<Object[]> findDisabledTimeRanges(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date
    );

    /**
     * Count disabled slots for a resource on a date.
     */
    @Query("SELECT COUNT(ds) FROM DisabledSlot ds WHERE ds.resource.id = :resourceId AND ds.disabledDate = :date")
    long countByResourceIdAndDate(@Param("resourceId") Long resourceId, @Param("date") LocalDate date);

    // ==================== END OPTIMIZED QUERIES ====================
}
