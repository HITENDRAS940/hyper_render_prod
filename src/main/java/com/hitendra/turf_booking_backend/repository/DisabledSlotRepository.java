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
}
