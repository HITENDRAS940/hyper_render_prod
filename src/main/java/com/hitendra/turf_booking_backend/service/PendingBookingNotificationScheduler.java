package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.entity.Booking;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PendingBookingNotificationScheduler {

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    @Value("${booking.pending-reminder.enabled:true}")
    private boolean pendingReminderEnabled;

    @Value("${booking.pending-reminder.delay-minutes:2}")
    private long delayMinutes;

    @Value("${booking.pending-reminder.batch-size:100}")
    private int batchSize;

    @Scheduled(cron = "${booking.pending-reminder.cron:0 */2 * * * *}")
    @Transactional
    public void sendPendingBookingReminders() {
        if (!pendingReminderEnabled) {
            return;
        }

        Instant threshold = Instant.now().minusSeconds(delayMinutes * 60);
        List<Booking> bookings = bookingRepository.findPendingBookingsForReminder(
                threshold,
                PageRequest.of(0, batchSize));

        if (bookings.isEmpty()) {
            return;
        }

        int sentCount = 0;
        Instant reminderAt = Instant.now();

        for (Booking booking : bookings) {
            try {
                if (notificationService.sendPendingBookingReminder(booking)) {
                    sentCount++;
                }

                // Mark processed so each booking gets at most one reminder.
                booking.setPendingReminderSentAt(reminderAt);
            } catch (Exception e) {
                log.error("Failed pending reminder flow for bookingId={}", booking.getId(), e);
            }
        }

        bookingRepository.saveAll(bookings);
        log.info("Pending reminder scheduler processed {} bookings, sent {} notifications", bookings.size(), sentCount);
    }
}

