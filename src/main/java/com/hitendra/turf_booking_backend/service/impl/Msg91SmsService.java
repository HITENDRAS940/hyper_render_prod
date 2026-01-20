package com.hitendra.turf_booking_backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitendra.turf_booking_backend.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class Msg91SmsService implements SmsService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final boolean isConfigured;

    @Value("${msg91.auth-key:not_configured}")
    private String authKey;

    @Value("${msg91.template-id:not_configured}")
    private String templateId;

    @Value("${msg91.sender-id:EZTURF}")
    private String senderId;

    // MSG91 OTP API endpoints
    private static final String MSG91_SEND_OTP_URL = "https://control.msg91.com/api/v5/otp";
    private static final String MSG91_VERIFY_OTP_URL = "https://control.msg91.com/api/v5/otp/verify";
    private static final String MSG91_RESEND_OTP_URL = "https://control.msg91.com/api/v5/otp/retry";
    private static final String MSG91_FLOW_API_URL = "https://control.msg91.com/api/v5/flow/";

    public Msg91SmsService(@Value("${msg91.auth-key:not_configured}") String authKey) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.isConfigured = !"not_configured".equals(authKey);

        if (isConfigured) {
            log.info("MSG91 SMS Service initialized successfully");
        } else {
            log.warn("MSG91 not configured. SMS will be logged to console only.");
        }
    }

    @Override
    public void sendOtp(String phone, String otp) {
        try {
            if (!isConfigured) {
                log.info("📱 [DEV MODE] OTP for {}: {}", phone, otp);
                return;
            }

            // Format phone number (remove + if present, ensure country code)
            String formattedPhone = formatPhoneNumber(phone);

            // Build request for MSG91 OTP API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authkey", authKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("template_id", templateId);
            requestBody.put("mobile", formattedPhone);
            requestBody.put("otp", otp);
            requestBody.put("sender", senderId);
            requestBody.put("otp_length", 6);
            requestBody.put("otp_expiry", 5); // 5 minutes

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    MSG91_SEND_OTP_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                String type = responseJson.has("type") ? responseJson.get("type").asText() : "";

                if ("success".equalsIgnoreCase(type)) {
                    log.info("OTP sent successfully to {}", formattedPhone);
                } else {
                    String message = responseJson.has("message") ? responseJson.get("message").asText() : "Unknown error";
                    log.error("Failed to send OTP to {}: {}", formattedPhone, message);
                    // Fallback to console logging
                    log.info("📱 [FALLBACK] OTP for {}: {}", phone, otp);
                }
            } else {
                log.error("MSG91 API error: {}", response.getBody());
                log.info("📱 [FALLBACK] OTP for {}: {}", phone, otp);
            }

        } catch (Exception e) {
            log.error("Failed to send OTP via MSG91 to {}: {}", phone, e.getMessage(), e);
            // Fallback to console logging
            log.info("📱 [FALLBACK] OTP for {}: {}", phone, otp);
        }
    }

    @Override
    public void sendBookingConfirmation(String phone, String message) {
        try {
            if (!isConfigured) {
                log.info("📱 [DEV MODE] Booking confirmation to {}: {}", phone, message);
                return;
            }

            // Use MSG91 Flow API for transactional messages
            String formattedPhone = formatPhoneNumber(phone);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authkey", authKey);

            // Using Flow API for booking confirmation
            // You need to create a flow/template in MSG91 dashboard first
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("flow_id", templateId); // Use a different template for booking
            requestBody.put("sender", senderId);

            Map<String, String> recipient = new HashMap<>();
            recipient.put("mobiles", formattedPhone);
            recipient.put("message", message);

            requestBody.put("recipients", List.of(recipient));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    MSG91_FLOW_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Booking confirmation sent successfully to {}", formattedPhone);
            } else {
                log.error("Failed to send booking confirmation: {}", response.getBody());
                log.info("📱 [FALLBACK] Booking confirmation to {}: {}", phone, message);
            }

        } catch (Exception e) {
            log.error("Failed to send booking confirmation via MSG91 to {}: {}", phone, e.getMessage(), e);
            log.info("📱 [FALLBACK] Booking confirmation to {}: {}", phone, message);
        }
    }

    /**
     * Format phone number for MSG91 (requires country code without +)
     * Example: +919876543210 -> 919876543210
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null) return "";

        // Remove all non-digit characters
        String digits = phone.replaceAll("[^0-9]", "");

        // If number doesn't start with country code, assume India (91)
        if (digits.length() == 10) {
            digits = "91" + digits;
        }

        return digits;
    }

    /**
     * Verify OTP using MSG91 (optional - if you want to use MSG91's verification)
     * Note: Currently the app generates and verifies OTP internally
     */
    public boolean verifyOtpWithMsg91(String phone, String otp) {
        try {
            if (!isConfigured) {
                log.warn("MSG91 not configured for OTP verification");
                return false;
            }

            String formattedPhone = formatPhoneNumber(phone);
            String url = MSG91_VERIFY_OTP_URL + "?mobile=" + formattedPhone + "&otp=" + otp;

            HttpHeaders headers = new HttpHeaders();
            headers.set("authkey", authKey);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                String type = responseJson.has("type") ? responseJson.get("type").asText() : "";
                return "success".equalsIgnoreCase(type);
            }

            return false;
        } catch (Exception e) {
            log.error("Failed to verify OTP with MSG91: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Resend OTP using MSG91
     */
    public void resendOtp(String phone, String retryType) {
        try {
            if (!isConfigured) {
                log.info("📱 [DEV MODE] Resend OTP request for {}", phone);
                return;
            }

            String formattedPhone = formatPhoneNumber(phone);
            // retryType can be "text" or "voice"
            String url = MSG91_RESEND_OTP_URL + "?mobile=" + formattedPhone + "&retrytype=" + retryType;

            HttpHeaders headers = new HttpHeaders();
            headers.set("authkey", authKey);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("OTP resent successfully to {}", formattedPhone);
            } else {
                log.error("Failed to resend OTP: {}", response.getBody());
            }

        } catch (Exception e) {
            log.error("Failed to resend OTP via MSG91 to {}: {}", phone, e.getMessage(), e);
        }
    }
}

