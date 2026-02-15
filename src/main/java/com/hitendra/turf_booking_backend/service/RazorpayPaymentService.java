package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.payment.*;
import com.hitendra.turf_booking_backend.entity.Booking;
import com.hitendra.turf_booking_backend.entity.BookingStatus;
import com.hitendra.turf_booking_backend.entity.PaymentStatus;
import com.hitendra.turf_booking_backend.exception.PaymentException;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayPaymentService {

    private final RazorpayClient razorpayClient;
    private final BookingRepository bookingRepository;
    private final SmsService smsService;
    private final EmailService emailService;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    /**
     * Create a Razorpay order for a booking with strict state validation.
     * Enforces state machine: PENDING -> AWAITING_CONFIRMATION
     *
     * CRITICAL VALIDATION:
     * 1. Only allows payment if booking status allows it
     * 2. Checks for conflicting slot payments (another user with IN_PROGRESS or SUCCESS)
     * 3. Prevents duplicate orders for same booking
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RazorpayOrderResponse createOrder(Long bookingId) {
        log.info("Creating Razorpay order for booking ID: {}", bookingId);

        // Fetch booking with pessimistic lock to prevent race conditions
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new PaymentException("Booking not found with ID: " + bookingId));

        // STATE VALIDATION: Only allow payment for specific statuses
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.error("Cannot create order for already confirmed booking: {}", bookingId);
            throw new PaymentException("Booking is already confirmed");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            log.error("Cannot create order for cancelled/expired booking: {}", bookingId);
            throw new PaymentException("Booking is " + booking.getStatus().name().toLowerCase() + " and cannot be paid");
        }

        // ═══════════════════════════════════════════════════════════════════════
        // CRITICAL: CHECK BOOKING EXPIRY (5-MINUTE WINDOW)
        // ═══════════════════════════════════════════════════════════════════════
        // A pending status booking must complete payment within 5 minutes of creation.
        // If the Razorpay order is being created after 5 minutes, the booking has expired.
        // User should try booking again.

        checkBookingExpiry(booking);

        // DUPLICATE PREVENTION: Check if order already exists and payment is in progress
        if (booking.getRazorpayOrderId() != null &&
            booking.getPaymentStatusEnum() == PaymentStatus.IN_PROGRESS) {
            log.warn("Razorpay order already exists for booking: {}. Returning existing order.", bookingId);
            // Use onlineAmountPaid for the order amount (this is what user pays online)
            double onlineAmount = booking.getOnlineAmountPaid().doubleValue();
            return RazorpayOrderResponse.builder()
                    .orderId(booking.getRazorpayOrderId())
                    .amount(String.valueOf((int)(onlineAmount * 100)))
                    .currency("INR")
                    .receipt("receipt_" + bookingId)
                    .status("created")
                    .bookingId(bookingId)
                    .keyId(keyId)
                    .build();
        }

        // ═══════════════════════════════════════════════════════════════════════
        // CRITICAL: CHECK FOR CONFLICTING SLOT PAYMENTS
        // ═══════════════════════════════════════════════════════════════════════
        // Before allowing this user to initiate payment, check if another user
        // has already locked this slot with an IN_PROGRESS or SUCCESS payment

        List<Booking> conflictingBookings = bookingRepository.findConflictingPaymentLockedBookings(
                booking.getResource().getId(),
                booking.getBookingDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getId()
        );

        if (!conflictingBookings.isEmpty()) {
            log.error("Slot is already locked by another booking. Conflicting booking IDs: {}",
                    conflictingBookings.stream().map(Booking::getId).toList());
            throw new PaymentException("This slot is no longer available. Another user is completing payment.");
        }

        try {
            // Calculate online amount to charge (use onlineAmountPaid if set, else fallback to total)
            double onlineAmount = booking.getOnlineAmountPaid() != null
                    ? booking.getOnlineAmountPaid().doubleValue()
                    : booking.getAmount(); // Fallback to total if not set

            // Create Razorpay order
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int) (onlineAmount * 100)); // Amount in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_" + bookingId);
            orderRequest.put("payment_capture", 1); // Auto-capture payment

            // Add notes for reference
            JSONObject notes = new JSONObject();
            notes.put("booking_id", bookingId);
            notes.put("user_email", booking.getUser().getEmail());
            notes.put("service_name", booking.getService().getName());
            orderRequest.put("notes", notes);

            Order order = razorpayClient.orders.create(orderRequest);

            // STATE TRANSITION: Update booking state
            booking.setRazorpayOrderId(order.get("id"));
            booking.setPaymentStatusEnum(PaymentStatus.IN_PROGRESS);
            booking.setStatus(BookingStatus.AWAITING_CONFIRMATION);
            booking.setPaymentInitiatedAt(Instant.now());

            // Set timeout (10 minutes from now)
            booking.setLockExpiresAt(Instant.now().plusSeconds(600));

            bookingRepository.save(booking);

            log.info("Razorpay order created successfully. Order ID: {}, Booking ID: {}, Status: AWAITING_CONFIRMATION",
                    order.get("id"), bookingId);

            // Return response for frontend
            return RazorpayOrderResponse.builder()
                    .orderId(order.get("id"))
                    .amount(order.get("amount").toString())
                    .currency(order.get("currency"))
                    .receipt(order.get("receipt"))
                    .status(order.get("status"))
                    .bookingId(bookingId)
                    .keyId(keyId)
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for booking ID: {}", bookingId, e);

            // Mark payment as failed
            booking.setPaymentStatusEnum(PaymentStatus.FAILED);
            bookingRepository.save(booking);

            throw new PaymentException("Failed to create Razorpay order: " + e.getMessage());
        }
    }

    /**
     * Verify payment signature from frontend (OPTIONAL - webhook is source of truth).
     * This method is kept for backward compatibility but does NOT confirm booking.
     * Booking confirmation happens ONLY via webhook.
     */
    @Transactional(readOnly = true)
    public boolean verifyPaymentSignature(RazorpayPaymentVerificationRequest request) {
        log.info("Verifying payment signature for order ID: {} (verification only, booking confirmed by webhook)",
                request.getRazorpayOrderId());

        try {
            // Verify signature
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValidSignature = Utils.verifyPaymentSignature(options, keySecret);

            if (!isValidSignature) {
                log.error("Invalid payment signature for order ID: {}", request.getRazorpayOrderId());
                return false;
            }

            log.info("✅ Payment signature verified for order ID: {}. Awaiting webhook confirmation.",
                    request.getRazorpayOrderId());

            return true;

        } catch (RazorpayException e) {
            log.error("Failed to verify payment signature", e);
            throw new PaymentException("Failed to verify payment: " + e.getMessage());
        }
    }

    /**
     * Handle Razorpay webhook events with idempotency.
     * This is the ONLY way bookings should be confirmed.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void handleWebhook(String payload, String signature) {
        log.info("Received Razorpay webhook");

        try {
            // Verify webhook signature
            boolean isValidSignature = verifyWebhookSignature(payload, signature);

            if (!isValidSignature) {
                log.error("Invalid webhook signature");
                throw new PaymentException("Invalid webhook signature");
            }

            // Parse webhook payload
            JSONObject webhook = new JSONObject(payload);
            String event = webhook.getString("event");
            JSONObject payloadObj = webhook.getJSONObject("payload");
            JSONObject paymentEntity = payloadObj.getJSONObject("payment").getJSONObject("entity");

            String orderId = paymentEntity.getString("order_id");
            String paymentId = paymentEntity.getString("id");
            String status = paymentEntity.getString("status");
            String method = paymentEntity.optString("method", "unknown");

            log.info("Webhook event: {}, Order ID: {}, Payment ID: {}, Status: {}",
                    event, orderId, paymentId, status);

            // Fetch booking with lock
            Booking booking = bookingRepository.findByRazorpayOrderId(orderId)
                    .orElseThrow(() -> new PaymentException("Booking not found for order ID: " + orderId));

            // Handle payment.captured event
            if ("payment.captured".equals(event)) {
                // IDEMPOTENCY: Check if already confirmed
                if (booking.getStatus() == BookingStatus.CONFIRMED) {
                    log.info("Booking already confirmed (idempotent webhook call). Booking ID: {}", booking.getId());
                    return;
                }

                // STATE VALIDATION: Only confirm if in correct state
                if (booking.getStatus() != BookingStatus.AWAITING_CONFIRMATION) {
                    log.warn("Received payment.captured for booking not in AWAITING_CONFIRMATION. Status: {}, Booking ID: {}",
                            booking.getStatus(), booking.getId());
                    // Still process if payment succeeded (late webhook scenario)
                }

                // STATE TRANSITION: AWAITING_CONFIRMATION -> CONFIRMED
                booking.setRazorpayPaymentId(paymentId);
                booking.setPaymentMethod(method);
                booking.setPaymentStatusEnum(PaymentStatus.SUCCESS);
                booking.setPaymentTime(Instant.now());
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setLockExpiresAt(null); // Remove timeout

                bookingRepository.save(booking);

                // ═══════════════════════════════════════════════════════════════════════
                // CRITICAL: EXPIRE ALL ABANDONED BOOKINGS FOR THIS SLOT
                // ═══════════════════════════════════════════════════════════════════════
                // When this booking is confirmed, all other pending bookings for the same
                // slot must be marked as EXPIRED to ensure fair slot allocation.

                expireAbandonedBookingsForSlot(booking);

                // Send notifications (SMS and Email)
                sendBookingNotifications(booking);


                log.info("✅ Payment captured successfully. Booking confirmed: ID={}, PaymentID={}",
                        booking.getId(), paymentId);
            }

            // Handle payment.failed event
            if ("payment.failed".equals(event)) {
                // IDEMPOTENCY: Check if already cancelled
                if (booking.getStatus() == BookingStatus.CANCELLED) {
                    log.info("Booking already cancelled (idempotent webhook call). Booking ID: {}", booking.getId());
                    return;
                }

                // STATE TRANSITION: AWAITING_CONFIRMATION -> CANCELLED
                booking.setRazorpayPaymentId(paymentId);
                booking.setPaymentStatusEnum(PaymentStatus.FAILED);
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setLockExpiresAt(null); // Remove timeout

                bookingRepository.save(booking);

                log.info("❌ Payment failed. Booking cancelled: ID={}", booking.getId());
            }

        } catch (Exception e) {
            log.error("Failed to process webhook", e);
            throw new PaymentException("Failed to process webhook: " + e.getMessage());
        }
    }

    /**
     * Verify webhook signature
     */
    private boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(webhookSecret.getBytes("UTF-8"), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] hash = sha256_HMAC.doFinal(payload.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            String expectedSignature = hexString.toString();
            return expectedSignature.equals(signature);

        } catch (Exception e) {
            log.error("Failed to verify webhook signature", e);
            return false;
        }
    }

    /**
     * Expire all abandoned bookings for a slot when another booking is confirmed.
     *
     * SCENARIO:
     * - User A creates booking (PENDING, NOT_STARTED) and leaves without paying
     * - User B creates booking (PENDING, NOT_STARTED) for same slot
     * - User B initiates payment (AWAITING_CONFIRMATION, IN_PROGRESS)
     * - User B payment succeeds (CONFIRMED, SUCCESS)
     * - User A's booking must be marked EXPIRED immediately
     *
     * This ensures fair slot allocation and prevents abandoned bookings from blocking slots.
     */
    private void expireAbandonedBookingsForSlot(Booking confirmedBooking) {
        log.info("Expiring abandoned bookings for slot: resourceId={}, date={}, time={}-{}",
                confirmedBooking.getResource().getId(),
                confirmedBooking.getBookingDate(),
                confirmedBooking.getStartTime(),
                confirmedBooking.getEndTime());

        // Find all abandoned bookings for this slot
        List<Booking> abandonedBookings = bookingRepository.findAbandonedBookingsForSlot(
                confirmedBooking.getResource().getId(),
                confirmedBooking.getBookingDate(),
                confirmedBooking.getStartTime(),
                confirmedBooking.getEndTime(),
                confirmedBooking.getId()
        );

        if (abandonedBookings.isEmpty()) {
            log.info("No abandoned bookings to expire for this slot");
            return;
        }

        // Collect booking IDs for bulk update
        List<Long> bookingIdsToExpire = abandonedBookings.stream()
                .map(Booking::getId)
                .toList();

        // Bulk expire all abandoned bookings
        int expiredCount = bookingRepository.expireAbandonedBookings(bookingIdsToExpire);

        log.info("✅ Expired {} abandoned booking(s) for slot. Booking IDs: {}",
                expiredCount, bookingIdsToExpire);

        // Log individual expiries for audit trail
        for (Booking abandoned : abandonedBookings) {
            log.info("Expired booking: ID={}, User={}, Status={} -> EXPIRED",
                    abandoned.getId(),
                    abandoned.getUser().getEmail(),
                    abandoned.getStatus());
        }
    }

    /**
     * Check if booking has expired (5-minute window from creation).
     * Throws PaymentException with BOOKING_EXPIRED error code if expired.
     */
    private void checkBookingExpiry(Booking booking) {
        final long EXPIRY_WINDOW_SECONDS = 300; // 5 minutes

        // Calculate time elapsed since booking creation
        Instant now = Instant.now();
        long elapsedSeconds = now.getEpochSecond() - booking.getCreatedAt().getEpochSecond();

        if (elapsedSeconds >= EXPIRY_WINDOW_SECONDS) {
            log.warn("Booking expired: ID={}, CreatedAt={}, Now={}, ElapsedSeconds={}, ExpiryWindow={}s",
                    booking.getId(),
                    booking.getCreatedAt(),
                    now,
                    elapsedSeconds,
                    EXPIRY_WINDOW_SECONDS);

            throw new PaymentException(
                    "Booking expired. Your booking is no longer valid. Please try booking again.",
                    PaymentException.PaymentErrorCode.BOOKING_EXPIRED,
                    "Booking created at " + booking.getCreatedAt() + " is now expired (5-minute window exceeded)"
            );
        }

        // Log remaining time for monitoring
        long remainingSeconds = EXPIRY_WINDOW_SECONDS - elapsedSeconds;
        log.info("Booking ID={} is valid. Remaining time for payment: {}s", booking.getId(), remainingSeconds);
    }

    /**
     * Get booking and payment status for frontend polling.
     * Used during payment loading screens.
     */
    @Transactional(readOnly = true)
    public com.hitendra.turf_booking_backend.dto.booking.BookingStatusResponse getBookingStatus(Long bookingId) {
        log.info("Fetching booking status for booking ID: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new PaymentException("Booking not found with ID: " + bookingId));

        // Determine if booking is in final state
        boolean isCompleted = booking.getStatus() == BookingStatus.CONFIRMED ||
                              booking.getStatus() == BookingStatus.CANCELLED ||
                              booking.getStatus() == BookingStatus.EXPIRED ||
                              booking.getPaymentStatusEnum() == PaymentStatus.SUCCESS ||
                              booking.getPaymentStatusEnum() == PaymentStatus.FAILED;

        // Generate appropriate message
        String message = generateStatusMessage(booking);

        return com.hitendra.turf_booking_backend.dto.booking.BookingStatusResponse.builder()
                .bookingId(booking.getId())
                .reference(booking.getReference())
                .bookingStatus(booking.getStatus())
                .paymentStatus(booking.getPaymentStatusEnum())
                .razorpayOrderId(booking.getRazorpayOrderId())
                .razorpayPaymentId(booking.getRazorpayPaymentId())
                .paymentInitiatedAt(booking.getPaymentInitiatedAt())
                .paymentCompletedAt(booking.getPaymentTime())
                .message(message)
                .isCompleted(isCompleted)
                .build();
    }

    /**
     * Generate user-friendly status message
     */
    private String generateStatusMessage(Booking booking) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return "Payment successful! Booking confirmed.";
        }
        if (booking.getStatus() == BookingStatus.AWAITING_CONFIRMATION) {
            return "Processing payment... Please wait.";
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return "Booking cancelled. Payment failed or was not completed.";
        }
        if (booking.getStatus() == BookingStatus.EXPIRED) {
            return "Booking expired. Payment timeout exceeded.";
        }
        if (booking.getStatus() == BookingStatus.PENDING) {
            return "Booking created. Proceed to payment.";
        }
        return "Booking status: " + booking.getStatus().name();
    }

    /**
     * Fetch payment details
     */
    public Payment getPaymentDetails(String paymentId) {
        try {
            return razorpayClient.payments.fetch(paymentId);
        } catch (RazorpayException e) {
            log.error("Failed to fetch payment details for payment ID: {}", paymentId, e);
            throw new PaymentException("Failed to fetch payment details: " + e.getMessage());
        }
    }

    /**
     * Fetch order details
     */
    public Order getOrderDetails(String orderId) {
        try {
            return razorpayClient.orders.fetch(orderId);
        } catch (RazorpayException e) {
            log.error("Failed to fetch order details for order ID: {}", orderId, e);
            throw new PaymentException("Failed to fetch order details: " + e.getMessage());
        }
    }

    /**
     * Send booking notifications via SMS and Email
     */
    private void sendBookingNotifications(Booking booking) {
        String slotDetails = booking.getStartTime() + " - " + booking.getEndTime();
        String resourceName = booking.getResource() != null ? booking.getResource().getName() : "";

        if (booking.getUser() != null) {
            // Send SMS notification
            String userMessage = String.format(
                    "Booking confirmed! Service: %s, Resource: %s, Date: %s, Slots: %s, Total: ₹%.2f, Reference: %s",
                    booking.getService().getName(),
                    resourceName,
                    booking.getBookingDate(),
                    slotDetails,
                    booking.getAmount(),
                    booking.getReference()
            );
            smsService.sendBookingConfirmation(booking.getUser().getPhone(), userMessage);

            // Send detailed email notification
            if (booking.getUser().getEmail() != null && !booking.getUser().getEmail().isEmpty()) {
                try {
                    EmailService.BookingDetails bookingDetails = EmailService.BookingDetails.builder()
                            .reference(booking.getReference())
                            .serviceName(booking.getService().getName())
                            .resourceName(resourceName)
                            .bookingDate(booking.getBookingDate().toString())
                            .startTime(booking.getStartTime().toString())
                            .endTime(booking.getEndTime().toString())
                            .totalAmount(booking.getAmount())
                            .onlineAmountPaid(booking.getOnlineAmountPaid() != null ? booking.getOnlineAmountPaid().doubleValue() : booking.getAmount())
                            .venueAmountDue(booking.getVenueAmountDue() != null ? booking.getVenueAmountDue().doubleValue() : 0.0)
                            .location(booking.getService().getLocation())
                            .contactNumber(booking.getService().getContactNumber())
                            .build();

                    emailService.sendBookingConfirmationEmail(
                            booking.getUser().getEmail(),
                            booking.getUser().getName(),
                            bookingDetails
                    );
                } catch (Exception e) {
                    log.error("Failed to send booking confirmation email for booking {}", booking.getReference(), e);
                }
            }
        }

        log.info("Booking notifications sent for {}", booking.getReference());
    }
}
