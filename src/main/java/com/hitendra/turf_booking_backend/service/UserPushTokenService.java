package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.entity.UserPushToken;
import com.hitendra.turf_booking_backend.repository.UserPushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPushTokenService {

    private final UserPushTokenRepository userPushTokenRepository;

    @Transactional
    public void saveToken(Long userId, String token) {
        if (userPushTokenRepository.existsByUserIdAndToken(userId, token)) {
            return;
        }

        try {
            UserPushToken pushToken = UserPushToken.builder()
                    .userId(userId)
                    .token(token)
                    .build();
            userPushTokenRepository.save(pushToken);
            log.info("Saved push token for userId: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to save user push token: {}", e.getMessage());
        }
    }

    @Transactional
    public void removeToken(String token) {
        userPushTokenRepository.deleteByToken(token);
        log.info("Removed user push token globally: {}", token);
    }

    @Transactional
    public void removeTokenForUser(Long userId, String token) {
        userPushTokenRepository.deleteByUserIdAndToken(userId, token);
        log.info("Removed user push token for userId: {}", userId);
    }

    public List<UserPushToken> getTokensForUsers(List<Long> userIds) {
        return userPushTokenRepository.findByUserIdIn(userIds);
    }

    public List<UserPushToken> getAllTokens() {
        return userPushTokenRepository.findAll();
    }
}

