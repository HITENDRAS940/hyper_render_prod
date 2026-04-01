package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.entity.AdminPushToken;
import com.hitendra.turf_booking_backend.entity.Booking;
import com.hitendra.turf_booking_backend.entity.UserPushToken;
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
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final AdminPushTokenService adminPushTokenService;
    private final UserPushTokenService userPushTokenService;
    private final ServiceRepository serviceRepository;
    private final com.hitendra.turf_booking_backend.repository.BookingRepository bookingRepository;
    private final RestTemplate restTemplate;

    @Value("${expo.push.url:https://exp.host/--/api/v2/push/send}")
    private String expoPushUrl;

    @Value("${expo.push.enabled:true}")
    private boolean expoPushEnabled;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public void notifyAdmins(Booking bookingEventPayload) {
        if (!expoPushEnabled) {
            log.info("Expo push notifications are disabled. Skipping notification for booking: {}", bookingEventPayload.getId());
            return;
        }

        try {
            // Re-fetch booking with all relationships to avoid LazyInitializationException in async thread
            Booking booking = bookingRepository.findByIdWithAllRelationships(bookingEventPayload.getId())
                    .orElse(bookingEventPayload);

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

            String serviceName = booking.getService().getName();
            String userName = booking.getUser() != null && booking.getUser().getName() != null
                    ? booking.getUser().getName() : "Customer";
            String date = booking.getBookingDate().toString();
            String time = booking.getStartTime() + " - " + booking.getEndTime();

            for (AdminPushToken token : tokens) {
                Map<String, Object> message = new HashMap<>();
                message.put("to", token.getToken());
                message.put("title", "New Booking @ " + serviceName);
                message.put("body", String.format("%s booked for %s at %s", userName, date, time));

                Map<String, Object> data = new HashMap<>();
                data.put("bookingId", booking.getId());
                data.put("serviceId", booking.getService().getId());
                message.put("data", data);

                pushMessages.add(message);
            }

            sendExpoPushNotifications(pushMessages, adminPushTokenService::removeToken);

        } catch (Exception e) {
            log.error("Failed to notify admins for booking: {}", bookingEventPayload.getId(), e);
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public boolean sendPendingBookingReminder(Booking bookingPayload) {
        if (!expoPushEnabled) {
            return false;
        }

        try {
            Booking booking = bookingRepository.findByIdWithAllRelationships(bookingPayload.getId())
                    .orElse(bookingPayload);

            if (booking.getUser() == null) {
                return false;
            }

            List<UserPushToken> userTokens = userPushTokenService.getTokensForUsers(List.of(booking.getUser().getId()));
            if (userTokens.isEmpty()) {
                return false;
            }

            String userName = booking.getUser().getName() != null ? booking.getUser().getName() : "Customer";
            String serviceName = booking.getService() != null ? booking.getService().getName() : "your venue";

            List<Map<String, Object>> messages = new ArrayList<>();
            for (UserPushToken token : userTokens) {
                Map<String, Object> message = new HashMap<>();
                message.put("to", token.getToken());
                message.put("title", "Booking pending");
                message.put("body", String.format(
                        "Hi %s, your booking at %s is pending. Complete payment to confirm your slot.",
                        userName,
                        serviceName));

                Map<String, Object> data = new HashMap<>();
                data.put("bookingId", booking.getId());
                data.put("reference", booking.getReference());
                data.put("type", "PENDING_BOOKING_REMINDER");
                message.put("data", data);
                messages.add(message);
            }

            return sendExpoPushNotifications(messages, userPushTokenService::removeToken) > 0;
        } catch (Exception e) {
            log.error("Failed to send pending reminder for booking: {}", bookingPayload.getId(), e);
            return false;
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public int sendCustomNotificationToUsers(String title, String body, List<Long> userIds, Map<String, Object> data) {
        if (!expoPushEnabled) {
            return 0;
        }

        List<UserPushToken> tokens = (userIds == null || userIds.isEmpty())
                ? userPushTokenService.getAllTokens()
                : userPushTokenService.getTokensForUsers(userIds);

        if (tokens.isEmpty()) {
            return 0;
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        for (UserPushToken token : tokens) {
            Map<String, Object> message = new HashMap<>();
            message.put("to", token.getToken());
            message.put("title", title);
            message.put("body", body);
            message.put("data", data != null ? data : new HashMap<>());
            messages.add(message);
        }

        return sendExpoPushNotifications(messages, userPushTokenService::removeToken);
    }

    private int sendExpoPushNotifications(List<Map<String, Object>> messages, Consumer<String> invalidTokenHandler) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");

        HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(messages, headers);

        try {
            // Expo supports batch sending
            Map<String, Object> response = restTemplate.postForObject(expoPushUrl, request, Map.class);
            handleExpoResponse(response, messages, invalidTokenHandler);
            return messages.size();
        } catch (Exception e) {
            log.error("Error sending push notifications to Expo", e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleExpoResponse(Map<String, Object> response,
                                    List<Map<String, Object>> sentMessages,
                                    Consumer<String> invalidTokenHandler) {
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
                    invalidTokenHandler.accept(invalidToken);
                } else if ("InvalidCredentials".equals(error) || "MismatchSenderId".equals(error)) {
                    log.error("Expo/FCM Configuration Error: {}. Please check your Expo Dashboard credentials for this project.", message);
                }
            }
        }

        log.info("Processed {} push notification responses", data.size());
    }
}
