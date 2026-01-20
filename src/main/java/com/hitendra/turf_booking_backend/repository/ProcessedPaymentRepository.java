package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.ProcessedPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedPaymentRepository extends JpaRepository<ProcessedPayment, Long> {

    boolean existsByCfPaymentId(String cfPaymentId);

    Optional<ProcessedPayment> findByCfPaymentId(String cfPaymentId);

    Optional<ProcessedPayment> findByOrderId(String orderId);

    Optional<ProcessedPayment> findByInternalBookingId(String internalBookingId);
}

