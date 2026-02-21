package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.invoice.InvoiceReceiveRequest;
import com.hitendra.turf_booking_backend.entity.Booking;
import com.hitendra.turf_booking_backend.entity.Invoice;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import com.hitendra.turf_booking_backend.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;

    /**
     * Receives an invoice callback from the invoice generator service and persists it.
     * Idempotent — if an invoice for this booking already exists it is updated in-place.
     */
    @Transactional
    public void receiveInvoice(InvoiceReceiveRequest request) {
        Long bookingId = request.getBookingId();
        log.info("Receiving invoice for booking ID: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

        // Idempotency — update if already exists
        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseGet(Invoice::new);

        invoice.setBooking(booking);
        invoice.setInvoiceUrl(request.getInvoiceURL());

        // ── Snapshot booking data at receipt time ──────────────────────────
        invoice.setBookingReference(booking.getReference());
        invoice.setBookingDate(booking.getBookingDate());
        invoice.setStartTime(booking.getStartTime());
        invoice.setEndTime(booking.getEndTime());
        invoice.setTotalAmount(booking.getAmount());
        invoice.setOnlineAmountPaid(booking.getOnlineAmountPaid());
        invoice.setVenueAmountDue(booking.getVenueAmountDue());
        invoice.setPaymentMethod(booking.getPaymentMethod());
        invoice.setPaymentMode(booking.getPaymentMode());
        invoice.setRazorpayPaymentId(booking.getRazorpayPaymentId());

        // ── Service / Resource ─────────────────────────────────────────────
        if (booking.getService() != null) {
            invoice.setServiceId(booking.getService().getId());
            invoice.setServiceName(booking.getService().getName());
        }
        if (booking.getResource() != null) {
            invoice.setResourceId(booking.getResource().getId());
            invoice.setResourceName(booking.getResource().getName());
        }

        // ── User ──────────────────────────────────────────────────────────
        if (booking.getUser() != null) {
            invoice.setUserId(booking.getUser().getId());
            invoice.setUserName(booking.getUser().getName());
            invoice.setUserEmail(booking.getUser().getEmail());
            invoice.setUserPhone(booking.getUser().getPhone());
        }

        invoiceRepository.save(invoice);
        log.info("✅ Invoice saved for booking ID: {}, URL: {}", bookingId, request.getInvoiceURL());
    }

    /**
     * Checks whether an invoice exists for the given booking ID.
     *
     * @param bookingId the booking ID to check
     * @return true if an invoice exists, false otherwise
     */
    public boolean checkInvoiceExists(Long bookingId) {
        return invoiceRepository.existsByBookingId(bookingId);
    }

    /**
     * Returns the invoice download URL for the given booking ID.
     * Throws a RuntimeException if no invoice is found.
     *
     * @param bookingId the booking ID whose invoice URL is requested
     * @return the invoice URL string
     */
    public String getInvoiceUrl(Long bookingId) {
        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Invoice not found for booking ID: " + bookingId));
        return invoice.getInvoiceUrl();
    }
}

