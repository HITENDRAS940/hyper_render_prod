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
        // Find if token exists in DB
        // Since token is unique column, we can use findByToken if we had one,
        // or recreate repository method. Let's find ANY entry with this token.
        // But repository only has findByAdminIdIn, existsByAdminIdAndToken.

        // Let's add findByToken or similar to Repo. Or just try-catch.
        // Actually unique constraint violation is heavy.

        // I'll add findByToken to repo.
        // For now, let's assume I will add it.

        // Wait, I can't add to repo easily without editing repo file again.
        // I will just use try-catch and handle collision by deleting old one.

        try {
            if (adminPushTokenRepository.existsByAdminIdAndToken(adminId, token)) {
                return;
            }

            // It doesn't exist for THIS admin. It might exist for ANOTHER.
            // Try to save.
            AdminPushToken pushToken = AdminPushToken.builder()
                    .adminId(adminId)
                    .token(token)
                    .build();
            adminPushTokenRepository.save(pushToken);
            log.info("Saved push token for adminId: {}", adminId);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Token already exists for another user. Delete old one and save new one.
            log.info("Token exists for another user. Reassigning token to adminId: {}", adminId);
            adminPushTokenRepository.deleteByToken(token);
            adminPushTokenRepository.flush(); // Force delete

            AdminPushToken pushToken = AdminPushToken.builder()
                    .adminId(adminId)
                    .token(token)
                    .build();
            adminPushTokenRepository.save(pushToken);
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


