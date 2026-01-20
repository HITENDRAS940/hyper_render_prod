package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.booking.BookingPaymentRequest;
import com.hitendra.turf_booking_backend.dto.booking.BookingPaymentResponse;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.exception.WalletException;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import com.hitendra.turf_booking_backend.repository.ServiceResourceRepository;
import com.hitendra.turf_booking_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for booking with integrated wallet and online payment.
 * Handles all payment scenarios: wallet-only, online-only, wallet+online.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingPaymentService {

    private final BookingRepository bookingRepository;
    private final ServiceResourceRepository serviceResourceRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    // Razorpay is now handled separately via RazorpayPaymentService

    private static final String BOOKING_PREFIX = "BK_";

    /**
     * Create a booking with payment.
     * Handles wallet-only, online-only, and wallet+online payment methods.
     *
     * @param userId  User making the booking
     * @param request Booking details and payment preferences
     * @return Response with booking and payment details
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingPaymentResponse createBookingWithPayment(Long userId, BookingPaymentRequest request) {
        log.info("Creating booking: userId={}, resourceId={}, date={}, time={}-{}, method={}",
                userId, request.getResourceId(), request.getBookingDate(),
                request.getStartTime(), request.getEndTime(), request.getPaymentMethod());

        // 1. Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // 2. Validate resource exists and is enabled
        ServiceResource resource = serviceResourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Resource not found: " + request.getResourceId()));

        if (!resource.isEnabled()) {
            throw new RuntimeException("Resource is not available for booking");
        }

        // 3. Check for double booking with lock
        List<Booking> overlapping = bookingRepository.findOverlappingBookingsWithLock(
                request.getResourceId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (!overlapping.isEmpty()) {
            log.warn("Slot already booked: resourceId={}, date={}, time={}-{}",
                    request.getResourceId(), request.getBookingDate(),
                    request.getStartTime(), request.getEndTime());
            throw new RuntimeException("This slot is already booked");
        }

        // 4. Generate unique booking reference
        String bookingReference = BOOKING_PREFIX + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        // 5. Calculate wallet and online amounts
        BigDecimal totalAmount = request.getTotalAmount();
        BigDecimal walletAmount = BigDecimal.ZERO;
        BigDecimal onlineAmount = totalAmount;

        PaymentMethod paymentMethod = request.getPaymentMethod();

        if (paymentMethod == PaymentMethod.WALLET_ONLY) {
            walletAmount = totalAmount;
            onlineAmount = BigDecimal.ZERO;
        } else if (paymentMethod == PaymentMethod.WALLET_PLUS_ONLINE) {
            walletAmount = request.getWalletAmount() != null ? request.getWalletAmount() : BigDecimal.ZERO;
            // Ensure wallet amount doesn't exceed total
            if (walletAmount.compareTo(totalAmount) > 0) {
                walletAmount = totalAmount;
            }
            onlineAmount = totalAmount.subtract(walletAmount);
        }
        // For ONLINE_ONLY, walletAmount stays 0

        // 6. Validate wallet balance if using wallet
        if (walletAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (!walletService.hasSufficientBalance(userId, walletAmount)) {
                throw new WalletException("Insufficient wallet balance. Required: " + walletAmount,
                        WalletException.WalletErrorCode.INSUFFICIENT_BALANCE);
            }
        }

        // 7. Create booking with PENDING status
        Booking booking = Booking.builder()
                .user(user)
                .service(resource.getService())
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .bookingDate(request.getBookingDate())
                .amount(totalAmount.doubleValue())
                .status(BookingStatus.PENDING)
                .reference(bookingReference)
                .createdAt(Instant.now())
                .paymentMethodType(paymentMethod)
                .walletAmountUsed(walletAmount)
                .onlineAmountPaid(BigDecimal.ZERO) // Will be updated after online payment
                .build();

        booking = bookingRepository.save(booking);
        log.info("Created pending booking: id={}, reference={}", booking.getId(), bookingReference);

        // 8. Process wallet payment if applicable
        Long walletTxId = null;
        if (walletAmount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                WalletTransaction walletTx = walletService.debitWallet(
                        userId,
                        walletAmount,
                        WalletTransactionSource.BOOKING,
                        bookingReference,
                        "Booking payment: " + bookingReference
                );
                walletTxId = walletTx.getId();
                booking.setWalletTransactionId(walletTxId);
                log.info("Wallet debited for booking: txId={}, amount={}", walletTxId, walletAmount);
            } catch (WalletException e) {
                // Rollback booking
                bookingRepository.delete(booking);
                throw e;
            }
        }

        // 9. If wallet covers full amount, confirm booking immediately
        if (onlineAmount.compareTo(BigDecimal.ZERO) <= 0) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentStatusEnum(com.hitendra.turf_booking_backend.entity.PaymentStatus.SUCCESS);
            booking.setPaymentSource(PaymentSource.WALLET);
            bookingRepository.save(booking);

            log.info("Booking confirmed with wallet only: bookingId={}", booking.getId());

            return BookingPaymentResponse.builder()
                    .bookingId(booking.getId())
                    .bookingReference(bookingReference)
                    .status(BookingStatus.CONFIRMED)
                    .paymentMethod(paymentMethod)
                    .totalAmount(totalAmount)
                    .walletAmountUsed(walletAmount)
                    .onlineAmountDue(BigDecimal.ZERO)
                    .walletTransactionId(walletTxId)
                    .message("Booking confirmed successfully")
                    .build();
        }

        // 10. Save booking and instruct frontend to create Razorpay order
        // Note: Razorpay order creation is now handled by frontend calling /api/razorpay/create-order/{bookingId}
        bookingRepository.save(booking);

        log.info("Booking saved for online payment: bookingId={}, amount={}", booking.getId(), onlineAmount);

        return BookingPaymentResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(bookingReference)
                .status(BookingStatus.PENDING)
                .paymentMethod(paymentMethod)
                .totalAmount(totalAmount)
                .walletAmountUsed(walletAmount)
                .onlineAmountDue(onlineAmount)
                .paymentOrderId(null) // Will be set when Razorpay order is created
                .paymentSessionId(null) // Will be set when Razorpay order is created
                .walletTransactionId(walletTxId)
                .message("Please proceed to payment. Call /api/razorpay/create-order/" + booking.getId())
                .build();
    }

    /**
     * Process payment webhook for booking confirmation.
     * DEPRECATED: This method is for Cashfree webhooks.
     * Use RazorpayPaymentService.handleWebhook() instead.
     */
    @Deprecated
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean processBookingPaymentWebhook(String orderId, String paymentStatus, String paymentId) {
        log.warn("Legacy webhook method called. Use RazorpayPaymentService.handleWebhook() instead.");

        // For backward compatibility, try to find booking by reference
        String bookingReference = orderId.replace("PAY_", "");
        Booking booking = bookingRepository.findByReferenceWithLock(bookingReference).orElse(null);

        if (booking == null) {
            log.warn("Booking not found for order: {}", orderId);
            return false;
        }

        // Idempotency check
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.warn("Booking already confirmed: {}", bookingReference);
            return true;
        }

        if ("SUCCESS".equalsIgnoreCase(paymentStatus) || "PAID".equalsIgnoreCase(paymentStatus)) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentStatusEnum(com.hitendra.turf_booking_backend.entity.PaymentStatus.SUCCESS);
            booking.setRazorpayPaymentId(paymentId);
            booking.setPaymentTime(Instant.now());

            BigDecimal walletUsed = booking.getWalletAmountUsed() != null ?
                    booking.getWalletAmountUsed() : BigDecimal.ZERO;
            BigDecimal onlinePaid = BigDecimal.valueOf(booking.getAmount()).subtract(walletUsed);
            booking.setOnlineAmountPaid(onlinePaid);

            if (walletUsed.compareTo(BigDecimal.ZERO) > 0) {
                booking.setPaymentSource(PaymentSource.WALLET);
            } else {
                booking.setPaymentSource(PaymentSource.ONLINE);
            }

            bookingRepository.save(booking);
            log.info("Booking confirmed via legacy webhook: reference={}", bookingReference);
            return true;

        } else if ("FAILED".equalsIgnoreCase(paymentStatus) || "CANCELLED".equalsIgnoreCase(paymentStatus)) {
            if (booking.getWalletAmountUsed() != null &&
                    booking.getWalletAmountUsed().compareTo(BigDecimal.ZERO) > 0) {
                try {
                    walletService.creditWallet(
                            booking.getUser().getId(),
                            booking.getWalletAmountUsed(),
                            WalletTransactionSource.REFUND,
                            "REFUND_" + bookingReference,
                            "Refund for failed booking: " + bookingReference
                    );
                } catch (Exception e) {
                    log.error("Failed to refund wallet: reference={}", bookingReference, e);
                }
            }

            booking.setStatus(BookingStatus.CANCELLED);
            booking.setPaymentStatusEnum(com.hitendra.turf_booking_backend.entity.PaymentStatus.FAILED);
            bookingRepository.save(booking);

            log.info("Booking cancelled due to payment failure: reference={}", bookingReference);
            return true;
        }

        return false;
    }

    /**
     * Cancel booking and process refund.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cancelBookingWithRefund(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        // Validate user owns the booking
        if (!booking.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED ||
                booking.getStatus() == BookingStatus.REFUNDED) {
            throw new RuntimeException("Booking is already cancelled/refunded");
        }

        // Process wallet refund (prefer wallet refund over bank)
        BigDecimal refundAmount = BigDecimal.valueOf(booking.getAmount());
        String refundReference = "REFUND_" + booking.getReference();

        // Check idempotency
        if (walletService.getBalance(userId).compareTo(BigDecimal.ZERO) >= 0) {
            // Credit full amount to wallet as refund
            walletService.creditWallet(
                    userId,
                    refundAmount,
                    WalletTransactionSource.REFUND,
                    refundReference,
                    "Booking cancellation refund: " + booking.getReference()
            );
        }

        booking.setStatus(BookingStatus.REFUNDED);
        bookingRepository.save(booking);

        log.info("Booking cancelled with refund: bookingId={}, amount={}", bookingId, refundAmount);
    }

    /**
     * Create Cashfree payment order for booking.
     * DEPRECATED: Replaced with Razorpay payment gateway.
     * Use RazorpayPaymentService.createOrder() instead.
     */
    @Deprecated
    private String createCashfreeOrder(String orderId, BigDecimal amount, User user, String bookingRef) {
        throw new UnsupportedOperationException(
            "Cashfree payment gateway is no longer supported. Use Razorpay instead.");
    }
}

