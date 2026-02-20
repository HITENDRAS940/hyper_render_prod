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
     * Auto-expire PENDING bookings that are older than 5 minutes.
     * A PENDING booking that has not been confirmed/paid within 5 minutes is considered stale and expired.
     * Runs every minute.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000) // Every 1 minute
    @Transactional
    public void expireStalePendingBookings() {
        Instant threshold = Instant.now().minusSeconds(300); // 5 minutes ago

        List<Booking> staleBookings = bookingRepository.findStalePendingBookings(threshold);

        if (staleBookings.isEmpty()) {
            log.debug("No stale PENDING bookings found");
            return;
        }

        log.info("Found {} stale PENDING booking(s) to expire", staleBookings.size());

        for (Booking booking : staleBookings) {
            try {
                booking.setStatus(BookingStatus.EXPIRED);
                log.info("⏰ Expired stale PENDING booking: ID={}, Reference={}, CreatedAt={}",
                        booking.getId(), booking.getReference(), booking.getCreatedAt());
            } catch (Exception e) {
                log.error("Failed to expire stale PENDING booking ID: {}", booking.getId(), e);
            }
        }

        bookingRepository.saveAll(staleBookings);
        log.info("Stale PENDING booking expiry completed. Expired {} booking(s)", staleBookings.size());
    }

    /**
     * Auto-cancel bookings stuck in AWAITING_CONFIRMATION state beyond timeout.
     * Runs every minute.
     *
     * Timeout: 10 minutes from payment initiation
     * OPTIMIZED: Uses database-level query instead of loading all bookings into memory.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000) // Every 1 minute
    @Transactional
    public void cancelExpiredPayments() {
        Instant now = Instant.now();

        log.debug("Running payment timeout scheduler check");

        // OPTIMIZED: Database-level filtering instead of findAll().stream().filter()
        List<Booking> expiredBookings = bookingRepository.findExpiredAwaitingConfirmationBookings(now);

        if (expiredBookings.isEmpty()) {
            log.debug("No expired payments found");
            return;
        }

        log.info("Found {} bookings with expired payment timeout", expiredBookings.size());

        // OPTIMIZED: Update all bookings in memory first, then save in batch
        for (Booking booking : expiredBookings) {
            try {
                // STATE TRANSITION: AWAITING_CONFIRMATION -> EXPIRED
                booking.setStatus(BookingStatus.EXPIRED);
                booking.setPaymentStatusEnum(PaymentStatus.FAILED);
                booking.setLockExpiresAt(null);

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

        // OPTIMIZED: Batch save all updated bookings at once
        bookingRepository.saveAll(expiredBookings);

        log.info("Payment timeout scheduler completed. Expired {} bookings", expiredBookings.size());
    }
}

