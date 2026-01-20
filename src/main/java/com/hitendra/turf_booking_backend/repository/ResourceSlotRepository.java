package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.ResourceSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ResourceSlotRepository extends JpaRepository<ResourceSlot, Long> {

    List<ResourceSlot> findByResourceIdOrderByDisplayOrderAsc(Long resourceId);

    List<ResourceSlot> findByResourceIdAndEnabledTrueOrderByDisplayOrderAsc(Long resourceId);

    Optional<ResourceSlot> findByResourceIdAndStartTimeAndEndTime(Long resourceId, LocalTime startTime, LocalTime endTime);

    @Query("SELECT rs FROM ResourceSlot rs WHERE rs.resource.id = :resourceId AND rs.enabled = true ORDER BY rs.startTime ASC")
    List<ResourceSlot> findEnabledSlotsByResourceId(@Param("resourceId") Long resourceId);

    @Query("SELECT rs FROM ResourceSlot rs WHERE rs.resource.service.id = :serviceId AND rs.enabled = true ORDER BY rs.resource.id, rs.startTime ASC")
    List<ResourceSlot> findEnabledSlotsByServiceId(@Param("serviceId") Long serviceId);

    @Modifying
    void deleteByResourceId(Long resourceId);

    boolean existsByResourceIdAndStartTimeAndEndTime(Long resourceId, LocalTime startTime, LocalTime endTime);
}

