package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.AdminPushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminPushTokenRepository extends JpaRepository<AdminPushToken, Long> {

    List<AdminPushToken> findByAdminIdIn(List<Long> adminIds);

    boolean existsByAdminIdAndToken(Long adminId, String token);

    void deleteByAdminIdAndToken(Long adminId, String token);

    void deleteByToken(String token);
}

