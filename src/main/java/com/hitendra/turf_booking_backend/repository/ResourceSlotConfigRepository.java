package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.ResourceSlotConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResourceSlotConfigRepository extends JpaRepository<ResourceSlotConfig, Long> {

    Optional<ResourceSlotConfig> findByResourceId(Long resourceId);

    Optional<ResourceSlotConfig> findByResourceIdAndEnabledTrue(Long resourceId);

    List<ResourceSlotConfig> findByResourceIdIn(java.util.List<Long> resourceIds);

    void deleteByResourceId(Long resourceId);
}
