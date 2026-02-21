package com.hitendra.turf_booking_backend.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Async listener that triggers invoice generation after a booking is confirmed.
 * Fires-and-forgets — any failure is logged but never propagates back to the webhook flow.
 */
@Component
@Slf4j
public class InvoiceEventListener {

    @Value("${invoice.generator.url}")
    private String invoiceApiUrl;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5_000;

    private final RestTemplate restTemplate;

    public InvoiceEventListener(@Qualifier("invoiceRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Async
    @EventListener
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        Long bookingId = event.getBooking().getId();
        log.info("📄 Triggering async invoice generation for booking ID: {}", bookingId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("bookingId", bookingId);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                restTemplate.postForEntity(invoiceApiUrl, request, String.class);
                log.info("✅ Invoice generation request sent successfully for booking ID: {} (attempt {})", bookingId, attempt);
                return;
            } catch (Exception e) {
                log.warn("⚠️ Invoice generation attempt {}/{} failed for booking ID: {} — Error: {}", attempt, MAX_RETRIES, bookingId, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("⚠️ Retry interrupted for booking ID: {}", bookingId);
                        return;
                    }
                }
            }
        }
        log.error("❌ Invoice generation failed after {} attempts for booking ID: {}", MAX_RETRIES, bookingId);
    }
}
