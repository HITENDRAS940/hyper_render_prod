package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.entity.Booking;
import com.hitendra.turf_booking_backend.entity.BookingStatus;
import com.hitendra.turf_booking_backend.entity.PaymentStatus;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduler service for handling payment timeouts.
 * Automatically cancels bookings stuck in AWAITING_CONFIRMATION beyond timeout.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutScheduler {

    private final BookingRepository bookingRepository;

    /**
     * Auto-cancel bookings stuck in AWAITING_CONFIRMATION state beyond timeout.
     * Runs every minute.
     *
     * Timeout: 10 minutes from payment initiation
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000) // Every 1 minute
    @Transactional
    public void cancelExpiredPayments() {
        Instant now = Instant.now();

        log.debug("Running payment timeout scheduler check");

        // Find all bookings in AWAITING_CONFIRMATION state with expired lock
        List<Booking> expiredBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.AWAITING_CONFIRMATION)
                .filter(b -> b.getLockExpiresAt() != null)
                .filter(b -> b.getLockExpiresAt().isBefore(now))
                .toList();

        if (expiredBookings.isEmpty()) {
            log.debug("No expired payments found");
            return;
        }

        log.info("Found {} bookings with expired payment timeout", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            try {
                // STATE TRANSITION: AWAITING_CONFIRMATION -> EXPIRED
                booking.setStatus(BookingStatus.EXPIRED);
                booking.setPaymentStatusEnum(PaymentStatus.FAILED);
                booking.setLockExpiresAt(null);

                bookingRepository.save(booking);

                log.info("⏰ Booking expired due to payment timeout. Booking ID: {}, Reference: {}, " +
                        "Initiated at: {}, Expired at: {}",
                        booking.getId(),
                        booking.getReference(),
                        booking.getPaymentInitiatedAt(),
                        booking.getLockExpiresAt());

            } catch (Exception e) {
                log.error("Failed to expire booking ID: {}", booking.getId(), e);
            }
        }

        log.info("Payment timeout scheduler completed. Expired {} bookings", expiredBookings.size());
    }
}

