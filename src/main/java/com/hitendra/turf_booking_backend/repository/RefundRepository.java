package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Refund;
import com.hitendra.turf_booking_backend.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Get refund ID by booking ID (minimal data fetch).
     */
    @Query("SELECT r.id FROM Refund r WHERE r.booking.id = :bookingId")
    Optional<Long> findRefundIdByBookingId(@Param("bookingId") Long bookingId);

    /**
     * Get refund status by booking ID (minimal data for status check).
     */
    @Query("SELECT r.status FROM Refund r WHERE r.booking.id = :bookingId")
    Optional<RefundStatus> findRefundStatusByBookingId(@Param("bookingId") Long bookingId);

    /**
     * Count refunds by status (for dashboard statistics).
     */
    @Query("SELECT COUNT(r) FROM Refund r WHERE r.status = :status")
    long countByStatus(@Param("status") RefundStatus status);

    /**
     * Get only IDs of pending refunds (for batch processing).
     */
    @Query("SELECT r.id FROM Refund r WHERE r.status IN ('INITIATED', 'PROCESSING') ORDER BY r.initiatedAt ASC")
    List<Long> findPendingRefundIds();

    // ==================== END OPTIMIZED QUERIES ====================

    // ==================== FINANCIAL AGGREGATION QUERIES ====================

    /**
     * Calculate Refunded Revenue.
     * Sum of refund_amount for a service within a date range.
     * Note: Refunds table now has service_id.
     */
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM Refund r WHERE r.service.id = :serviceId AND DATE(r.initiatedAt) BETWEEN :startDate AND :endDate")
    java.math.BigDecimal calculateRefundedRevenue(@Param("serviceId") Long serviceId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // ==================== END FINANCIAL AGGREGATION QUERIES ====================
}
