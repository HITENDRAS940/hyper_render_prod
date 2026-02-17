package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.entity.AdminPushToken;
import com.hitendra.turf_booking_backend.entity.Booking;
import com.hitendra.turf_booking_backend.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final AdminPushTokenService adminPushTokenService;
    private final ServiceRepository serviceRepository;
    private final RestTemplate restTemplate;

    @Value("${expo.push.url:https://exp.host/--/api/v2/push/send}")
    private String expoPushUrl;

    @Value("${expo.push.enabled:true}")
    private boolean expoPushEnabled;

    public void notifyAdmins(Booking booking) {
        if (!expoPushEnabled) {
            log.info("Expo push notifications are disabled. Skipping notification for booking: {}", booking.getId());
            return;
        }

        try {
            Long serviceId = booking.getService().getId();
            log.info("Notifying admins for booking ID: {}, Service ID: {}", booking.getId(), serviceId);

            // 2. Fetch adminIds linked to service
            List<Long> adminIds = serviceRepository.findAdminIdsByServiceId(serviceId);
            if (adminIds.isEmpty()) {
                log.info("No admins found for service ID: {}", serviceId);
                return;
            }

            // 3. Fetch all push tokens for those admins
            List<AdminPushToken> tokens = adminPushTokenService.getTokensForAdmins(adminIds);
            if (tokens.isEmpty()) {
                log.info("No push tokens found for admins: {}", adminIds);
                return;
            }

            // 4. Send push notifications in bulk
            List<Map<String, Object>> pushMessages = new ArrayList<>();
            for (AdminPushToken token : tokens) {
                Map<String, Object> message = new HashMap<>();
                message.put("to", token.getToken());
                message.put("title", "New Booking Confirmed");
                message.put("body", "Slot: " + booking.getStartTime() + " - " + booking.getEndTime());
                message.put("data", Map.of("bookingId", booking.getId()));
                pushMessages.add(message);
            }

            sendExpoPushNotifications(pushMessages, tokens);

        } catch (Exception e) {
            log.error("Failed to notify admins for booking: {}", booking.getId(), e);
        }
    }

    private void sendExpoPushNotifications(List<Map<String, Object>> messages, List<AdminPushToken> dbTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");

        HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(messages, headers);

        try {
            // Expo supports batch sending
            Map<String, Object> response = restTemplate.postForObject(expoPushUrl, request, Map.class);
            handleExpoResponse(response, messages);
        } catch (Exception e) {
            log.error("Error sending push notifications to Expo", e);
        }
    }

    private void handleExpoResponse(Map<String, Object> response, List<Map<String, Object>> sentMessages) {
        if (response == null || !response.containsKey("data")) {
            return;
        }

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

        // Iterate through response to find failures
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> result = data.get(i);
            String status = (String) result.get("status");

            if ("error".equals(status)) {
                String message = (String) result.get("message");
                Map<String, Object> details = (Map<String, Object>) result.get("details");
                String error = (details != null) ? (String) details.get("error") : null;

                log.warn("Push notification failed. Message: {}, Error: {}", message, error);

                if ("DeviceNotRegistered".equals(error)) {
                    // Token is invalid, remove it
                    String invalidToken = (String) sentMessages.get(i).get("to");
                    log.info("Removing invalid token: {}", invalidToken);
                    adminPushTokenService.removeToken(invalidToken);
                }
            }
        }

        log.info("Processed {} push notification responses", data.size());
    }
}



