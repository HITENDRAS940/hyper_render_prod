package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByPhoneAndUsedFalseOrderByExpiresAtDesc(String phone);

    Optional<Otp> findTopByEmailAndUsedFalseOrderByExpiresAtDesc(String email);
}

