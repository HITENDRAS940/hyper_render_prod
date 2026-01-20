package com.hitendra.turf_booking_backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitendra.turf_booking_backend.service.WalletPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhook controller for Cashfree wallet payment callbacks.
 * This endpoint is called by Cashfree when payment status changes.
 */
@RestController
@RequestMapping("/api/webhooks/wallet")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wallet Webhooks", description = "Wallet payment webhook endpoints")
public class WalletWebhookController {

    private final WalletPaymentService walletPaymentService;
    private final ObjectMapper objectMapper;

    /**
     * Handle Cashfree webhook for wallet top-up.
     * Verifies signature and processes payment status updates.
     */
    @PostMapping(value = "/cashfree", consumes = MediaType.ALL_VALUE)
    @Operation(
            summary = "Cashfree webhook",
            description = "Webhook endpoint for receiving wallet top-up payment notifications from Cashfree. Verifies signature and processes payment status."
    )
    public ResponseEntity<String> handleCashfreeWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-webhook-timestamp", required = false) String timestamp,
            @RequestHeader(value = "x-webhook-signature", required = false) String signature
    ) {

        log.info("Received wallet Cashfree webhook");
        log.debug("Timestamp: {}, Signature present: {}", timestamp, signature != null);

        // Verify webhook signature (mandatory for production)
        if (timestamp != null && signature != null) {
            boolean isValid = walletPaymentService.verifyWebhookSignature(timestamp, rawBody, signature);
            if (!isValid) {
                log.warn("Invalid wallet webhook signature received");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
            }
            log.info("Wallet webhook signature verified successfully");
        } else {
            log.warn("Wallet webhook received without signature headers - this should be blocked in production");
            // In production, you should reject requests without proper signature headers
            // For development/testing, we allow it to proceed
        }

        try {
            // Parse the JSON payload after signature verification
            Map<String, Object> payload = objectMapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {});

            // Process the webhook asynchronously to respond quickly
            // Note: In production, consider using async processing or message queue
            walletPaymentService.processTopupWebhookFromData(payload);

            // Respond immediately with 200 OK (within ~50ms requirement)
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error processing wallet webhook payload: {}", e.getMessage(), e);
            // Still return 200 to prevent Cashfree from retrying
            // Log the error for investigation
            return ResponseEntity.ok("Received");
        }
    }

    /**
     * Alternative webhook endpoint that accepts reference and status as query parameters.
     * Useful for testing or simple payment gateway integrations.
     */
    @PostMapping("/confirm")
    @Operation(summary = "Confirm wallet top-up", description = "Confirm wallet top-up via webhook with reference")
    public ResponseEntity<String> confirmTopup(
            @RequestParam String reference,
            @RequestParam String status) {

        log.info("Received wallet confirmation: reference={}, status={}", reference, status);

        boolean processed = walletPaymentService.processTopupWebhook(reference, status);

        if (processed) {
            return ResponseEntity.ok("OK");
        } else {
            return ResponseEntity.badRequest().body("Failed to process");
        }
    }
}

