package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.entity.AdminPushToken;
import com.hitendra.turf_booking_backend.repository.AdminPushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPushTokenService {

    private final AdminPushTokenRepository adminPushTokenRepository;

    @Transactional
    public void saveToken(Long adminId, String token) {
        // First check if this specific admin already has this token (Idempotent check)
        if (adminPushTokenRepository.existsByAdminIdAndToken(adminId, token)) {
            // Token is already assigned to this admin. No action needed.
            return;
        }

        try {
            // 1. Remove token from ANY user who might have it (including current user if re-registering, or another user)
            // This ensures unique constraint won't be violated.
            // Using deleteByToken handles the removal cleanly.
            adminPushTokenRepository.deleteByToken(token);

            // Critical: Flush changes to DB immediately to avoid unique constraint violation on save
            adminPushTokenRepository.flush();

            // 2. Save new association
            AdminPushToken pushToken = AdminPushToken.builder()
                    .adminId(adminId)
                    .token(token)
                    .build();
            adminPushTokenRepository.save(pushToken);
            log.info("Saved push token for adminId: {}", adminId);

        } catch (Exception e) {
             log.warn("Failed to save push token: {}", e.getMessage());
        }
    }

    @Transactional
    public void removeToken(String token) {
        adminPushTokenRepository.deleteByToken(token);
        log.info("Removed push token: {}", token);
    }

    public List<AdminPushToken> getTokensForAdmins(List<Long> adminIds) {
        return adminPushTokenRepository.findByAdminIdIn(adminIds);
    }
}


