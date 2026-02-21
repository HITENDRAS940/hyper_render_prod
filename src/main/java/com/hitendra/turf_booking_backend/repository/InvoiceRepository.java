package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);
}

