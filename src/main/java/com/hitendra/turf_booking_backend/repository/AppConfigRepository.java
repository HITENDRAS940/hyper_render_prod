package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {
}

