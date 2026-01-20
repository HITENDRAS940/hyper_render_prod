package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.wallet.WalletTopupResponse;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.exception.WalletException;
import com.hitendra.turf_booking_backend.repository.UserRepository;
import com.hitendra.turf_booking_backend.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Service for wallet payment operations.
 * Handles top-up transaction creation and webhook-based confirmation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletPaymentService {

    private final WalletService walletService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    private static final String WALLET_TOPUP_PREFIX = "WALLET_TOPUP_";

    /**
     * Initiate a wallet top-up by creating a PENDING transaction.
     * Returns reference and amount for payment gateway integration.
     *
     * @param userId User ID
     * @param amount Amount to top-up
     * @return WalletTopupResponse with reference and amount
     */
    @Transactional
    public WalletTopupResponse initiateTopup(Long userId, BigDecimal amount) {
        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WalletException("User not found"));

        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException("Invalid top-up amount. Must be greater than zero.");
        }

        // Generate unique reference for this transaction
        String reference = WALLET_TOPUP_PREFIX + userId + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Create PENDING wallet transaction (does not update wallet balance yet)
        WalletTransaction pendingTx = walletService.createPendingCreditTransaction(
                userId,
                amount,
                WalletTransactionSource.TOPUP,
                reference,
                "Wallet top-up via payment gateway"
        );

        log.info("Created pending top-up transaction: userId={}, amount={}, reference={}",
                userId, amount, reference);

        return WalletTopupResponse.builder()
                .reference(reference)
                .amount(amount)
                .status("PENDING")
                .build();
    }

    /**
     * Process payment webhook and complete the top-up.
     * Searches for the transaction by reference, marks it SUCCESS, and credits the wallet.
     *
     * @param reference Transaction reference from payment gateway
     * @param paymentStatus Payment status from gateway
     * @return true if processed successfully
     */
    @Transactional
    public boolean processTopupWebhook(String reference, String paymentStatus) {
        if (reference == null || reference.isBlank()) {
            log.error("Webhook received with empty reference");
            return false;
        }

        // Only process wallet top-up references
        if (!reference.startsWith(WALLET_TOPUP_PREFIX)) {
            log.warn("Not a wallet top-up reference: {}", reference);
            return false;
        }

        try {
            // Check idempotency - if already processed, skip
            long successCount = walletTransactionRepository.countSuccessfulByReferenceId(reference);
            if (successCount > 0) {
                log.warn("Webhook already processed: reference={}", reference);
                return true;
            }

            if ("SUCCESS".equalsIgnoreCase(paymentStatus) || "PAID".equalsIgnoreCase(paymentStatus)) {
                // Find and confirm the pending transaction
                // This will mark transaction as SUCCESS and add amount to wallet
                walletService.confirmPendingTransaction(reference);
                log.info("Wallet top-up completed: reference={}", reference);
                return true;
            } else if ("FAILED".equalsIgnoreCase(paymentStatus) || "CANCELLED".equalsIgnoreCase(paymentStatus)) {
                // Mark transaction as failed (wallet balance not affected)
                walletService.failPendingTransaction(reference, "Payment " + paymentStatus);
                log.info("Wallet top-up failed: reference={}, status={}", reference, paymentStatus);
                return true;
            }

            log.warn("Unknown payment status: reference={}, status={}", reference, paymentStatus);
            return false;

        } catch (Exception e) {
            log.error("Error processing wallet top-up webhook: reference={}, error={}",
                    reference, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process Cashfree webhook payload and complete the top-up.
     * Extracts reference and payment status from webhook data.
     *
     * @param webhookData Parsed webhook data containing order_id and payment_status
     * @return true if processed successfully
     */
    @Transactional
    public boolean processTopupWebhookFromData(java.util.Map<String, Object> webhookData) {
        String reference = (String) webhookData.get("order_id");
        String paymentStatus = (String) webhookData.get("payment_status");

        return processTopupWebhook(reference, paymentStatus);
    }

    /**
     * Get transaction status by reference.
     * Useful for frontend to poll transaction status.
     *
     * @param reference Transaction reference
     * @return Transaction status or null if not found
     */
    @Transactional(readOnly = true)
    public String getTransactionStatus(String reference) {
        List<WalletTransaction> transactions = walletTransactionRepository.findByReferenceId(reference);
        return transactions.isEmpty() ? null : transactions.get(0).getStatus().name();
    }

    /**
     * Verify Cashfree webhook signature using HMAC-SHA256.
     *
     * @param timestamp Webhook timestamp header
     * @param rawBody Raw request body
     * @param signature Webhook signature header
     * @return true if signature is valid
     */
    public boolean verifyWebhookSignature(String timestamp, String rawBody, String signature) {
        if (signature == null || timestamp == null) {
            log.warn("Missing signature or timestamp in webhook headers");
            return false;
        }

        try {
            // Cashfree signature format: timestamp + rawBody
            String data = timestamp + rawBody;
            String computedSignature = computeHmacSha256(data, razorpayKeySecret);

            boolean isValid = signature.equals(computedSignature);
            if (!isValid) {
                log.warn("Signature mismatch. Expected: {}, Got: {}", computedSignature, signature);
            }
            return isValid;
        } catch (Exception e) {
            log.error("Error verifying webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Compute HMAC-SHA256 hash.
     */
    private String computeHmacSha256(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
}

