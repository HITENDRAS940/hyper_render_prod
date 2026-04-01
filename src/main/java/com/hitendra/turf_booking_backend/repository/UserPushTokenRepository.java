package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.UserPushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPushTokenRepository extends JpaRepository<UserPushToken, Long> {

    List<UserPushToken> findByUserIdIn(List<Long> userIds);

    boolean existsByUserIdAndToken(Long userId, String token);

    void deleteByUserIdAndToken(Long userId, String token);

    void deleteByToken(String token);
}

