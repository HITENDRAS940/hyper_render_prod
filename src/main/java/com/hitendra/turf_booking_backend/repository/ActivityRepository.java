package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Optional<Activity> findByCode(String code);
}

