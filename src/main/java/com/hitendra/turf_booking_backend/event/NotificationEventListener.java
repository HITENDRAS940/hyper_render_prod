package com.hitendra.turf_booking_backend.event;

import com.hitendra.turf_booking_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Received BookingConfirmedEvent for booking ID: {}", event.getBooking().getId());
        notificationService.notifyAdmins(event.getBooking());
    }
}

