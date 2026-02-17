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
        // Check if this specific admin already has this token (Idempotent check)
        if (adminPushTokenRepository.existsByAdminIdAndToken(adminId, token)) {
            // Token is already assigned to this admin. No action needed.
            return;
        }

        try {
            // Save new association
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
        log.info("Removed push token globally: {}", token);
    }

    @Transactional
    public void removeTokenForAdmin(Long adminId, String token) {
        adminPushTokenRepository.deleteByAdminIdAndToken(adminId, token);
        log.info("Removed push token for adminId: {}", adminId);
    }

    public List<AdminPushToken> getTokensForAdmins(List<Long> adminIds) {
        return adminPushTokenRepository.findByAdminIdIn(adminIds);
    }
}


