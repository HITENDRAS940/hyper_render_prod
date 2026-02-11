package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.config.RefundConfig;
import com.hitendra.turf_booking_backend.dto.booking.CancellationResponseDto;
import com.hitendra.turf_booking_backend.dto.booking.RefundPreviewDto;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.exception.PaymentException;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import com.hitendra.turf_booking_backend.repository.RefundRepository;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Service for handling booking cancellations and refunds.
 * Implements industry-standard two-step cancellation flow:
 * 1. Preview refund (read-only)
 * 2. Confirm cancellation (with refund initiation)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final BookingRepository bookingRepository;
    private final RefundRepository refundRepository;
    private final RefundConfig refundConfig;
    private final AuthUtil authUtil;
    private final RazorpayClient razorpayClient;
    private final EmailService emailService;
    private final SmsService smsService;

    @Value("${pricing.online-payment-percent:30}")
    private Double onlinePaymentPercent;

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    /**
     * Get refund preview for a booking (READ-ONLY - no DB changes).
     * This is safe to call multiple times.
     *
     * @param bookingId booking to preview cancellation for
     * @return refund preview details
     */
    @Transactional(readOnly = true)
    public RefundPreviewDto getRefundPreview(Long bookingId) {
        log.info("Generating refund preview for booking ID: {}", bookingId);

        User currentUser = authUtil.getCurrentUser();
        if (currentUser == null) {
            throw new PaymentException("User not authenticated");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new PaymentException("Booking not found with ID: " + bookingId));

        // Validate user owns this booking
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new PaymentException("You are not authorized to cancel this booking");
        }

        return calculateRefundPreview(booking);
    }

    /**
     * Calculate refund preview (shared logic for preview and actual cancellation).
     * NO DATABASE WRITES.
     */
    private RefundPreviewDto calculateRefundPreview(Booking booking) {
        // Use the actual amount paid online (not total booking amount)
        // since users only pay online payment percentage upfront
        BigDecimal originalAmount;
        if (booking.getOnlineAmountPaid() != null) {
            // Use stored online amount paid
            originalAmount = booking.getOnlineAmountPaid();
        } else {
            // For backward compatibility: calculate from total amount using online payment percentage
            double totalAmount = booking.getAmount();
            double onlineAmount = Math.round(totalAmount * onlinePaymentPercent) / 100.0;
            originalAmount = BigDecimal.valueOf(onlineAmount);
        }

        String reference = booking.getReference();
        Long bookingId = booking.getId();

        // Check if refunds are enabled
        if (!refundConfig.isEnabled()) {
            return RefundPreviewDto.builder()
                    .canCancel(false)
                    .bookingId(bookingId)
                    .bookingReference(reference)
                    .originalAmount(originalAmount)
                    .refundAmount(BigDecimal.ZERO)
                    .refundPercent(0)
                    .deductionAmount(originalAmount)
                    .reasonNotAllowed("Cancellations are currently disabled")
                    .build();
        }

        // Check booking status - only CONFIRMED bookings can be cancelled for refund
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            String reason = getStatusReasonMessage(booking.getStatus());
            return RefundPreviewDto.builder()
                    .canCancel(false)
                    .bookingId(bookingId)
                    .bookingReference(reference)
                    .originalAmount(originalAmount)
                    .refundAmount(BigDecimal.ZERO)
                    .refundPercent(0)
                    .deductionAmount(originalAmount)
                    .reasonNotAllowed(reason)
                    .build();
        }

        // Check if refund already exists for this booking
        if (refundRepository.existsByBookingId(bookingId)) {
            return RefundPreviewDto.builder()
                    .canCancel(false)
                    .bookingId(bookingId)
                    .bookingReference(reference)
                    .originalAmount(originalAmount)
                    .refundAmount(BigDecimal.ZERO)
                    .refundPercent(0)
                    .deductionAmount(originalAmount)
                    .reasonNotAllowed("Refund has already been initiated for this booking")
                    .build();
        }

        // Calculate minutes before slot
        long minutesBeforeSlot = calculateMinutesBeforeSlot(booking);

        // Check if past booking cancellation is allowed
        if (minutesBeforeSlot < 0 && !refundConfig.isAllowPastCancellation()) {
            return RefundPreviewDto.builder()
                    .canCancel(false)
                    .bookingId(bookingId)
                    .bookingReference(reference)
                    .originalAmount(originalAmount)
                    .minutesBeforeSlot(minutesBeforeSlot)
                    .refundAmount(BigDecimal.ZERO)
                    .refundPercent(0)
                    .deductionAmount(originalAmount)
                    .reasonNotAllowed("Booking slot has already started or passed")
                    .build();
        }

        // Check minimum cancellation time
        if (minutesBeforeSlot < refundConfig.getMinMinutesForCancellation() &&
            refundConfig.getMinMinutesForCancellation() >= 0) {
            return RefundPreviewDto.builder()
                    .canCancel(false)
                    .bookingId(bookingId)
                    .bookingReference(reference)
                    .originalAmount(originalAmount)
                    .minutesBeforeSlot(minutesBeforeSlot)
                    .refundAmount(BigDecimal.ZERO)
                    .refundPercent(0)
                    .deductionAmount(originalAmount)
                    .reasonNotAllowed("Cancellation not allowed within " +
                            refundConfig.getMinMinutesForCancellation() + " minutes of slot start")
                    .build();
        }

        // Calculate refund percentage based on rules
        int refundPercent = refundConfig.getRefundPercent(minutesBeforeSlot);
        BigDecimal refundAmount = originalAmount
                .multiply(BigDecimal.valueOf(refundPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);


        BigDecimal deductionAmount = originalAmount.subtract(refundAmount);

        // Build user-friendly messages
        String message = buildRefundMessage(refundAmount, refundPercent, minutesBeforeSlot);
        String policyMessage = buildPolicyMessage(refundPercent, minutesBeforeSlot);

        return RefundPreviewDto.builder()
                .canCancel(true)
                .bookingId(bookingId)
                .bookingReference(reference)
                .originalAmount(originalAmount)
                .minutesBeforeSlot(minutesBeforeSlot)
                .refundPercent(refundPercent)
                .refundAmount(refundAmount)
                .deductionAmount(deductionAmount)
                .message(message)
                .policyMessage(policyMessage)
                .build();
    }

    /**
     * Cancel booking and initiate refund.
     * This re-calculates refund amount on server side (never trust frontend).
     *
     * @param bookingId booking to cancel
     * @return cancellation response with refund details
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CancellationResponseDto cancelBookingWithRefund(Long bookingId) {
        log.info("Processing cancellation with refund for booking ID: {}", bookingId);

        User currentUser = authUtil.getCurrentUser();
        if (currentUser == null) {
            throw new PaymentException("User not authenticated");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new PaymentException("Booking not found with ID: " + bookingId));

        // Validate user owns this booking
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new PaymentException("You are not authorized to cancel this booking");
        }

        // Re-calculate refund preview (NEVER TRUST FRONTEND VALUES)
        RefundPreviewDto preview = calculateRefundPreview(booking);

        if (!preview.isCanCancel()) {
            throw new PaymentException("Cancellation not allowed: " + preview.getReasonNotAllowed());
        }

        // Update booking status
        booking.setStatus(BookingStatus.CANCELLED_BY_USER);
        bookingRepository.save(booking);

        log.info("Booking {} cancelled by user. Refund amount: {}",
                booking.getReference(), preview.getRefundAmount());

        // If refund amount is 0, no refund record needed
        if (preview.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return CancellationResponseDto.builder()
                    .success(true)
                    .bookingId(bookingId)
                    .bookingReference(booking.getReference())
                    .bookingStatus(BookingStatus.CANCELLED_BY_USER)
                    .originalAmount(preview.getOriginalAmount())
                    .refundAmount(BigDecimal.ZERO)
                    .refundPercent(0)
                    .refundStatus(null)
                    .message("Booking cancelled. No refund applicable as per cancellation policy.")
                    .cancelledAt(Instant.now())
                    .build();
        }

        // Create refund record
        Refund refund = createRefundRecord(booking, preview);

        // Send refund notifications (email and SMS)
        sendRefundNotifications(booking, preview, refund);

        // Initiate Razorpay refund if payment was made via Razorpay
        if (booking.getRazorpayPaymentId() != null) {
            try {
                initiateRazorpayRefund(refund);
            } catch (Exception e) {
                log.error("Failed to initiate Razorpay refund for booking {}: {}",
                        booking.getReference(), e.getMessage());
                refund.setStatus(RefundStatus.FAILED);
                refund.setErrorMessage(e.getMessage());
                refundRepository.save(refund);
            }
        } else {
            // For other payment methods, mark for manual processing
            refund.setRefundType("MANUAL");
            refund.setStatus(RefundStatus.PROCESSING);
            refundRepository.save(refund);
            log.info("Refund marked for manual processing: booking={}, amount={}",
                    booking.getReference(), preview.getRefundAmount());
        }

        return CancellationResponseDto.builder()
                .success(true)
                .bookingId(bookingId)
                .bookingReference(booking.getReference())
                .bookingStatus(BookingStatus.CANCELLED_BY_USER)
                .originalAmount(preview.getOriginalAmount())
                .refundAmount(preview.getRefundAmount())
                .refundPercent(preview.getRefundPercent())
                .refundStatus(refund.getStatus())
                .refundId(refund.getId())
                .refundType(refund.getRefundType())
                .message(buildCancellationMessage(preview.getRefundAmount(), refund.getRefundType()))
                .cancelledAt(Instant.now())
                .build();
    }

    /**
     * Process refund for a booking that was already cancelled by admin or system.
     * This method creates a refund record and initiates the refund process
     * for bookings that were cancelled outside the normal user cancellation flow.
     *
     * @param booking The already cancelled booking
     * @param reason Reason for the refund
     * @return Refund entity that was created
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Refund processRefundForCancelledBooking(Booking booking, String reason) {
        log.info("Processing refund for cancelled booking: {}", booking.getReference());

        // Check if booking was paid and is eligible for refund
        if (booking.getOnlineAmountPaid() == null || booking.getOnlineAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Booking {} has no online payment, skipping refund", booking.getReference());
            return null;
        }

        // Check if refund already exists
        if (refundRepository.existsByBookingId(booking.getId())) {
            log.warn("Refund already exists for booking {}", booking.getReference());
            return null;
        }

        // Calculate refund preview
        RefundPreviewDto preview = calculateRefundPreview(booking);

        // If no refund is applicable, return null
        if (preview.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("No refund applicable for booking {}", booking.getReference());
            return null;
        }

        // Create refund record with admin reason
        String refundType = booking.getRazorpayPaymentId() != null ? "RAZORPAY" : "MANUAL";

        Refund refund = Refund.builder()
                .booking(booking)
                .user(booking.getUser())
                .originalAmount(preview.getOriginalAmount())
                .refundAmount(preview.getRefundAmount())
                .refundPercent(preview.getRefundPercent())
                .minutesBeforeSlot(preview.getMinutesBeforeSlot())
                .refundType(refundType)
                .razorpayPaymentId(booking.getRazorpayPaymentId())
                .reason(reason != null ? reason : "Booking cancelled by admin/system")
                .status(RefundStatus.INITIATED)
                .build();

        refund = refundRepository.save(refund);

        // Send refund notifications
        sendRefundNotifications(booking, preview, refund);

        // Initiate Razorpay refund if applicable
        if (booking.getRazorpayPaymentId() != null) {
            try {
                initiateRazorpayRefund(refund);
            } catch (Exception e) {
                log.error("Failed to initiate Razorpay refund for booking {}: {}",
                        booking.getReference(), e.getMessage());
                refund.setStatus(RefundStatus.FAILED);
                refund.setErrorMessage(e.getMessage());
                refundRepository.save(refund);
            }
        } else {
            // For other payment methods, mark for manual processing
            refund.setRefundType("MANUAL");
            refund.setStatus(RefundStatus.PROCESSING);
            refundRepository.save(refund);
        }

        log.info("Refund processed for booking {}: amount={}, status={}",
                booking.getReference(), preview.getRefundAmount(), refund.getStatus());

        return refund;
    }

    /**
     * Create refund record in database.
     */
    private Refund createRefundRecord(Booking booking, RefundPreviewDto preview) {
        String refundType = booking.getRazorpayPaymentId() != null ? "RAZORPAY" : "WALLET";

        Refund refund = Refund.builder()
                .booking(booking)
                .user(booking.getUser())
                .originalAmount(preview.getOriginalAmount())
                .refundAmount(preview.getRefundAmount())
                .refundPercent(preview.getRefundPercent())
                .minutesBeforeSlot(preview.getMinutesBeforeSlot())
                .refundType(refundType)
                .razorpayPaymentId(booking.getRazorpayPaymentId())
                .reason("User cancelled booking")
                .status(RefundStatus.INITIATED)
                .build();

        return refundRepository.save(refund);
    }

    /**
     * Initiate refund via Razorpay API.
     */
    private void initiateRazorpayRefund(Refund refund) throws RazorpayException {
        log.info("Initiating Razorpay refund for payment ID: {}, amount: {}",
                refund.getRazorpayPaymentId(), refund.getRefundAmount());

        JSONObject refundRequest = new JSONObject();
        refundRequest.put("amount", refund.getRefundAmount().multiply(BigDecimal.valueOf(100)).intValue());
        refundRequest.put("speed", "normal");
        refundRequest.put("notes", new JSONObject()
                .put("booking_id", refund.getBooking().getId())
                .put("booking_reference", refund.getBooking().getReference())
                .put("reason", "User cancelled booking")
        );

        com.razorpay.Refund razorpayRefund = razorpayClient.payments
                .refund(refund.getRazorpayPaymentId(), refundRequest);

        String refundId = razorpayRefund.get("id");
        String status = razorpayRefund.get("status");

        refund.setRazorpayRefundId(refundId);
        refund.setStatus(mapRazorpayRefundStatus(status));
        refund.setProcessedAt(Instant.now());
        refundRepository.save(refund);

        log.info("Razorpay refund initiated successfully. Refund ID: {}, Status: {}",
                refundId, status);
    }

    /**
     * Map Razorpay refund status to our RefundStatus enum.
     */
    private RefundStatus mapRazorpayRefundStatus(String razorpayStatus) {
        return switch (razorpayStatus.toLowerCase()) {
            case "processed" -> RefundStatus.SUCCESS;
            case "pending" -> RefundStatus.PROCESSING;
            case "failed" -> RefundStatus.FAILED;
            default -> RefundStatus.PROCESSING;
        };
    }

    /**
     * Calculate minutes before slot start time.
     */
    private long calculateMinutesBeforeSlot(Booking booking) {
        LocalDateTime slotStart = LocalDateTime.of(
                booking.getBookingDate(),
                booking.getStartTime()
        );

        LocalDateTime now = LocalDateTime.now(IST_ZONE);
        Duration duration = Duration.between(now, slotStart);

        return duration.toMinutes();
    }

    /**
     * Get reason message based on booking status.
     */
    private String getStatusReasonMessage(BookingStatus status) {
        return switch (status) {
            case PENDING -> "Booking is not yet confirmed. Cannot process refund.";
            case PAYMENT_PENDING -> "Payment is pending. Cannot cancel at this time.";
            case AWAITING_CONFIRMATION -> "Payment is being processed. Please wait for confirmation.";
            case COMPLETED -> "Booking has been completed. Cannot cancel.";
            case CANCELLED -> "Booking is already cancelled.";
            case CANCELLED_BY_USER -> "Booking has already been cancelled by user.";
            case EXPIRED -> "Booking has expired.";
            case REFUNDED -> "Booking has already been refunded.";
            default -> "Booking cannot be cancelled in current state: " + status;
        };
    }

    /**
     * Build user-friendly refund message.
     */
    private String buildRefundMessage(BigDecimal refundAmount, int refundPercent, long minutesBeforeSlot) {
        if (refundPercent == 0) {
            return "No refund will be provided for cancelling this booking.";
        }
        if (refundPercent == 100) {
            return String.format("You will receive a full refund of ₹%.2f", refundAmount);
        }
        return String.format("You will receive ₹%.2f (%d%% refund)", refundAmount, refundPercent);
    }

    /**
     * Build policy message explaining refund rules.
     */
    private String buildPolicyMessage(int refundPercent, long minutesBeforeSlot) {
        long hours = minutesBeforeSlot / 60;
        long mins = minutesBeforeSlot % 60;

        String timeStr;
        if (hours > 0) {
            timeStr = hours + " hour" + (hours > 1 ? "s" : "") +
                      (mins > 0 ? " " + mins + " min" : "");
        } else {
            timeStr = mins + " minutes";
        }

        if (refundPercent == 100) {
            return "Cancellation " + timeStr + " before slot - 100% refund applicable";
        } else if (refundPercent == 50) {
            return "Cancellation " + timeStr + " before slot - 50% refund applicable";
        } else if (refundPercent == 0) {
            return "Cancellation within 30 minutes of slot - No refund applicable";
        }
        return refundPercent + "% refund applicable as per cancellation policy";
    }

    /**
     * Build cancellation confirmation message.
     */
    private String buildCancellationMessage(BigDecimal refundAmount, String refundType) {
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "Booking cancelled successfully. No refund applicable.";
        }

        String destination = "RAZORPAY".equals(refundType)
                ? "your original payment method"
                : "your account (manual processing)";

        return String.format(
                "Booking cancelled successfully. ₹%.2f will be refunded to %s within 5-7 business days.",
                refundAmount, destination
        );
    }

    /**
     * Send refund initiated notifications via SMS and Email
     */
    private void sendRefundNotifications(Booking booking, RefundPreviewDto preview, Refund refund) {
        if (booking.getUser() == null) {
            log.warn("No user associated with booking {}, skipping notifications", booking.getReference());
            return;
        }

        try {
            // Send SMS notification
            String userMessage = String.format(
                    "Refund initiated! Booking: %s, Refund Amount: ₹%.2f, Status: %s, Reference: %s",
                    booking.getService().getName(),
                    preview.getRefundAmount(),
                    refund.getStatus(),
                    booking.getReference()
            );
            smsService.sendBookingConfirmation(booking.getUser().getPhone(), userMessage);

            // Send detailed email notification
            if (booking.getUser().getEmail() != null && !booking.getUser().getEmail().isEmpty()) {
                try {
                    EmailService.RefundDetails refundDetails = EmailService.RefundDetails.builder()
                            .bookingReference(booking.getReference())
                            .refundStatus(refund.getStatus().toString())
                            .originalAmount(preview.getOriginalAmount())
                            .refundAmount(preview.getRefundAmount())
                            .refundPercent(preview.getRefundPercent())
                            .build();

                    emailService.sendRefundInitiatedEmail(
                            booking.getUser().getEmail(),
                            booking.getUser().getName(),
                            refundDetails
                    );
                } catch (Exception e) {
                    log.error("Failed to send refund email for booking {}", booking.getReference(), e);
                }
            }

            log.info("Refund notifications sent for booking {}", booking.getReference());
        } catch (Exception e) {
            log.error("Failed to send refund notifications for booking {}", booking.getReference(), e);
        }
    }
}
