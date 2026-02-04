package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.ProcessedPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedPaymentRepository extends JpaRepository<ProcessedPayment, Long> {

    boolean existsByCfPaymentId(String cfPaymentId);

    Optional<ProcessedPayment> findByCfPaymentId(String cfPaymentId);

    Optional<ProcessedPayment> findByOrderId(String orderId);

    Optional<ProcessedPayment> findByInternalBookingId(String internalBookingId);

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Check if payment exists by order ID (faster than full entity load).
     */
    boolean existsByOrderId(String orderId);

    /**
     * Check if payment exists by internal booking ID (faster than full entity load).
     */
    boolean existsByInternalBookingId(String internalBookingId);

    /**
     * Get payment ID by order ID (minimal data fetch).
     */
    @Query("SELECT pp.id FROM ProcessedPayment pp WHERE pp.orderId = :orderId")
    Optional<Long> findPaymentIdByOrderId(@Param("orderId") String orderId);

    // ==================== END OPTIMIZED QUERIES ====================
}

