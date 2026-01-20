package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.entity.Booking;
import com.hitendra.turf_booking_backend.entity.BookingStatus;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════════
 * DISABLED: Auto-expiry scheduler is disabled
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * REASON:
 * - Admin will manually confirm or cancel all bookings
 * - Payment verification may take time
 * - Auto-expiry is not needed
 *
 * MANUAL BOOKING FLOW:
 * - User creates booking → status = PAYMENT_PENDING
 * - User makes payment
 * - Admin verifies payment (can take time)
 * - Admin confirms → status = CONFIRMED
 * - Admin can also cancel if payment invalid → status = CANCELLED
 *
 * FUTURE:
 * If auto-expiry is needed again, uncomment @Scheduled annotation below
 * ═══════════════════════════════════════════════════════════════════════════════════
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;

    /**
     * Auto-expire payment-pending bookings - DISABLED
     *
     * To enable: Uncomment @Scheduled annotation
     */
    // @Scheduled(fixedRate = 60000) // DISABLED - Admin will manually confirm/cancel
    @Transactional
    public void expirePaymentPendingBookings() {
        log.info("BookingExpiryScheduler is DISABLED - Admin manually confirms/cancels bookings");
        // No auto-expiry logic - admin will manually confirm or cancel bookings
    }
}

