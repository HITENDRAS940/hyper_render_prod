package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.entity.Booking;
import com.hitendra.turf_booking_backend.event.BookingConfirmedEvent;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Asynchronous event listener for booking confirmation events.
 *
 * ARCHITECTURE:
 * - Webhook confirms booking -> publishes BookingConfirmedEvent -> returns 200 OK immediately
 * - This listener picks up the event asynchronously
 * - Generates invoice in background thread
 * - Keeps webhook response fast (< 200ms)
 *
 * IDEMPOTENCY:
 * - InvoiceService checks if invoice already exists
 * - Safe to process duplicate events
 *
 * ERROR HANDLING:
 * - If invoice generation fails, booking is still confirmed
 * - Error is logged but doesn't affect booking status
 * - Admin can regenerate invoice manually later
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceGenerationEventListener {

    private final InvoiceService invoiceService;
    private final BookingRepository bookingRepository;

    /**
     * Handle BookingConfirmedEvent asynchronously.
     *
     * @Async ensures this runs in a separate thread pool
     * Webhook response is not blocked by invoice generation
     */
    @Async
    @EventListener
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        String threadName = Thread.currentThread().getName();
        log.info("🎯 Received BookingConfirmedEvent for booking ID: {} (async processing on thread: {})",
                event.getBookingId(), threadName);

        try {
            // Calculate event processing delay for monitoring
            long delay = System.currentTimeMillis() - event.getTimestamp();
            log.info("⏱ Event processing delay: {}ms", delay);

            // Fetch booking from database WITH all relationships eagerly loaded
            // This is CRITICAL to prevent LazyInitializationException in async thread
            Booking booking = bookingRepository.findByIdWithAllRelationships(event.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + event.getBookingId()));

            log.info("📦 Booking fetched with all relationships: ID={}, Status={}, Amount={}",
                    booking.getId(), booking.getStatus(), booking.getAmount());

            // Generate and store invoice (idempotent operation)
            log.info("🔄 Starting invoice generation for booking ID: {}", event.getBookingId());
            String invoiceUrl = invoiceService.generateAndStoreInvoice(booking);

            log.info("✅ Invoice generation completed for booking ID: {}. URL: {}",
                    event.getBookingId(), invoiceUrl);

        } catch (Exception e) {
            // Log error but don't throw - booking is already confirmed
            log.error("❌ Failed to generate invoice for booking ID: {} (Event: RazorpayPaymentId={}). " +
                            "Booking is still confirmed. Manager can regenerate invoice manually.",
                    event.getBookingId(), event.getRazorpayPaymentId(), e);

            // Print stack trace for debugging
            log.error("❌ Full stack trace:", e);
        }
    }
}

