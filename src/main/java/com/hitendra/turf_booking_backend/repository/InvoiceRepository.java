package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Invoice entity.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Check if invoice exists for a booking.
     * Used for idempotency checks.
     */
    boolean existsByBookingId(Long bookingId);

    /**
     * Find invoice by booking ID.
     */
    Optional<Invoice> findByBookingId(Long bookingId);

    /**
     * Find invoice by invoice number.
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}

