package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.ServiceSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceSlotRepository extends JpaRepository<ServiceSlot, Long> {

    List<ServiceSlot> findByServiceId(Long serviceId);

    List<ServiceSlot> findByServiceIdAndEnabledTrue(Long serviceId);

    Optional<ServiceSlot> findByServiceIdAndSlotId(Long serviceId, Long slotId);

    @Query("SELECT ss FROM ServiceSlot ss WHERE ss.service.id = :serviceId AND ss.slot.id = :slotId AND ss.enabled = true")
    Optional<ServiceSlot> findEnabledByServiceIdAndSlotId(@Param("serviceId") Long serviceId, @Param("slotId") Long slotId);
}
