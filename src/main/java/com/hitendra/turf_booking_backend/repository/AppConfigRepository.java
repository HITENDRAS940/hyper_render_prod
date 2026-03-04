package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {

    /** Always returns the single configuration row (lowest id). */
    @Query("SELECT c FROM AppConfig c ORDER BY c.id ASC LIMIT 1")
    Optional<AppConfig> findSingleton();
}
