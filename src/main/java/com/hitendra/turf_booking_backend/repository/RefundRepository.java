package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Refund;
import com.hitendra.turf_booking_backend.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    /**
     * Find refund by booking ID
     */
    Optional<Refund> findByBookingId(Long bookingId);

    /**
     * Find all refunds for a user
     */
    List<Refund> findByUserIdOrderByInitiatedAtDesc(Long userId);

    /**
     * Find refund by Razorpay refund ID
     */
    Optional<Refund> findByRazorpayRefundId(String razorpayRefundId);

    /**
     * Find all refunds with a specific status
     */
    List<Refund> findByStatus(RefundStatus status);

    /**
     * Check if a refund already exists for a booking
     */
    boolean existsByBookingId(Long bookingId);

    /**
     * Find pending refunds for processing
     */
    @Query("SELECT r FROM Refund r WHERE r.status IN ('INITIATED', 'PROCESSING') ORDER BY r.initiatedAt ASC")
    List<Refund> findPendingRefunds();
}
