package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Booking;
import com.hitendra.turf_booking_backend.entity.BookingStatus;
import com.hitendra.turf_booking_backend.entity.PaymentStatus;
import com.hitendra.turf_booking_backend.entity.ServiceResource;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByServiceId(Long serviceId);
    Page<Booking> findByServiceId(Long serviceId, Pageable pageable);

    List<Booking> findByUserId(Long userId);
    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    java.util.Optional<Booking> findByReference(String reference);

    /**
     * Find overlapping bookings for a resource on a date
     * Overlap condition: (StartA < EndB) and (EndA > StartB)
     *
     * IMPORTANT: Only considers a slot as "locked" if:
     * - status is CONFIRMED, or
     * - status is PENDING/AWAITING_CONFIRMATION AND payment_status is IN_PROGRESS or SUCCESS
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.resource.id = :resourceId 
        AND b.bookingDate = :date 
        AND b.startTime < :endTime AND b.endTime > :startTime 
        AND (
            b.status = 'CONFIRMED' 
            OR (
                b.status IN ('PENDING', 'AWAITING_CONFIRMATION') 
                AND b.paymentStatusEnum IN ('IN_PROGRESS', 'SUCCESS')
            )
        )
        """)
    List<Booking> findOverlappingBookings(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime
    );

    /**
     * Find bookings for a specific service and date
     */
    List<Booking> findByServiceIdAndBookingDate(Long serviceId, LocalDate bookingDate);

    /**
     * Find bookings for a specific resource and date
     */
    List<Booking> findByResourceIdAndBookingDate(Long resourceId, LocalDate bookingDate);

    /**
     * Find bookings by resource ID
     */
    List<Booking> findByResourceId(Long resourceId);

    /**
     * Find bookings by resource ID with pagination
     */
    Page<Booking> findByResourceId(Long resourceId, Pageable pageable);

    /**
     * Find bookings by resource ID and date with pagination
     */
    Page<Booking> findByResourceIdAndBookingDate(Long resourceId, LocalDate bookingDate, Pageable pageable);

    /**
     * Find overlapping bookings with pessimistic lock for double-booking prevention.
     *
     * IMPORTANT: Only considers a slot as "locked" if:
     * - status is CONFIRMED, or
     * - status is PENDING/AWAITING_CONFIRMATION AND payment_status is IN_PROGRESS or SUCCESS
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.resource.id = :resourceId 
        AND b.bookingDate = :date 
        AND b.startTime < :endTime AND b.endTime > :startTime 
        AND (
            b.status = 'CONFIRMED' 
            OR (
                b.status IN ('PENDING', 'AWAITING_CONFIRMATION') 
                AND b.paymentStatusEnum IN ('IN_PROGRESS', 'SUCCESS')
            )
        )
        """)
    List<Booking> findOverlappingBookingsWithLock(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime
    );

    /**
     * Find booking by reference with lock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.reference = :reference")
    java.util.Optional<Booking> findByReferenceWithLock(@Param("reference") String reference);

    /**
     * Find all active bookings for multiple resources on a specific date.
     * Used for aggregating availability across pooled resources.
     *
     * IMPORTANT: Only considers a slot as "booked" if:
     * - status is CONFIRMED, or
     * - status is PENDING/AWAITING_CONFIRMATION AND payment_status is IN_PROGRESS or SUCCESS
     *
     * Slots with payment_status = NOT_STARTED are NOT considered locked
     * (allows other users to book abandoned slots)
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.resource.id IN :resourceIds 
        AND b.bookingDate = :date 
        AND (
            b.status = 'CONFIRMED' 
            OR (
                b.status IN ('PENDING', 'AWAITING_CONFIRMATION') 
                AND b.paymentStatusEnum IN ('IN_PROGRESS', 'SUCCESS')
            )
        )
        """)
    List<Booking> findActiveBookingsForResources(
            @Param("resourceIds") List<Long> resourceIds,
            @Param("date") LocalDate date);

    /**
     * Find overlapping bookings across multiple resources with lock.
     * Used during slot booking to find which resources are available.
     *
     * IMPORTANT: Only considers a slot as "locked" if:
     * - status is CONFIRMED, or
     * - status is PENDING/AWAITING_CONFIRMATION AND payment_status is IN_PROGRESS or SUCCESS
     *
     * Slots with payment_status = NOT_STARTED are NOT considered locked
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.resource.id IN :resourceIds 
        AND b.bookingDate = :date 
        AND b.startTime < :endTime AND b.endTime > :startTime 
        AND (
            b.status = 'CONFIRMED' 
            OR (
                b.status IN ('PENDING', 'AWAITING_CONFIRMATION') 
                AND b.paymentStatusEnum IN ('IN_PROGRESS', 'SUCCESS')
            )
        )
        """)
    List<Booking> findOverlappingBookingsForResourcesWithLock(
            @Param("resourceIds") List<Long> resourceIds,
            @Param("date") LocalDate date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime);

    /**
     * Count overlapping bookings for a specific resource (for availability calculation).
     *
     * IMPORTANT: Only counts a slot as "locked" if:
     * - status is CONFIRMED, or
     * - status is PENDING/AWAITING_CONFIRMATION AND payment_status is IN_PROGRESS or SUCCESS
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b 
        WHERE b.resource.id = :resourceId 
        AND b.bookingDate = :date 
        AND b.startTime < :endTime AND b.endTime > :startTime 
        AND (
            b.status = 'CONFIRMED' 
            OR (
                b.status IN ('PENDING', 'AWAITING_CONFIRMATION') 
                AND b.paymentStatusEnum IN ('IN_PROGRESS', 'SUCCESS')
            )
        )
        """)
    int countOverlappingBookings(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime);

    /**
     * Check if booking exists by idempotency key (for idempotent retry handling).
     */
    @Query("SELECT b FROM Booking b WHERE b.idempotencyKey = :idempotencyKey")
    java.util.Optional<Booking> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    /**
     * Find IDs of services that have bookings overlapping the given time range.
     * Used for fast availability filtering.
     *
     * IMPORTANT: Only considers a slot as "locked" if:
     * - status is CONFIRMED, or
     * - status is PENDING/AWAITING_CONFIRMATION AND payment_status is IN_PROGRESS or SUCCESS
     */
    @Query("""
        SELECT DISTINCT b.service.id FROM Booking b 
        WHERE b.bookingDate = :date 
        AND (:activityCode IS NULL OR b.activityCode = :activityCode) 
        AND b.startTime < :endTime AND b.endTime > :startTime 
        AND (
            b.status = com.hitendra.turf_booking_backend.entity.BookingStatus.CONFIRMED 
            OR (
                b.status IN (com.hitendra.turf_booking_backend.entity.BookingStatus.PENDING, com.hitendra.turf_booking_backend.entity.BookingStatus.AWAITING_CONFIRMATION) 
                AND b.paymentStatusEnum IN (com.hitendra.turf_booking_backend.entity.PaymentStatus.IN_PROGRESS, com.hitendra.turf_booking_backend.entity.PaymentStatus.SUCCESS)
            )
        )
        """)
    List<Long> findBusyServiceIds(
            @Param("date") LocalDate date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime,
            @Param("activityCode") String activityCode
    );

    /**
     * Find all bookings for services created by a specific admin with specific status
     * Used for revenue reporting
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE b.service.createdBy.id = :adminId " +
           "AND b.status IN (:statuses)")
    List<Booking> findByServiceCreatedByIdAndStatusIn(
            @Param("adminId") Long adminId,
            @Param("statuses") List<BookingStatus> statuses
    );

    /**
     * Find all bookings for a specific service with specific status
     * Used for revenue reporting
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE b.service.id = :serviceId " +
           "AND b.status IN (:statuses)")
    List<Booking> findByServiceIdAndStatusIn(
            @Param("serviceId") Long serviceId,
            @Param("statuses") List<BookingStatus> statuses
    );

    /**
     * Get total revenue from confirmed bookings for a service in a date range
     */
    @Query("""
        SELECT COALESCE(SUM(b.amount), 0.0)
        FROM Booking b
        WHERE b.service.id = :serviceId
        AND b.status = 'CONFIRMED'
        AND b.bookingDate BETWEEN :startDate AND :endDate
    """)
    Double getTotalRevenueByServiceAndDateRange(
        @Param("serviceId") Long serviceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // ==================== MANUAL UPI SOFT-LOCKING METHODS ====================

    /**
     * Find active soft-locked booking for a slot (for resume booking and double-booking prevention).
     * A booking is considered active if:
     * - status = PAYMENT_PENDING
     * - lockExpiresAt > now
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.resource.id = :resourceId
        AND b.bookingDate = :date
        AND b.status = 'PAYMENT_PENDING'
        AND b.lockExpiresAt > :now
        AND b.startTime < :endTime
        AND b.endTime > :startTime
        ORDER BY b.createdAt DESC
    """)
    List<Booking> findActiveSoftLockedBookings(
        @Param("resourceId") Long resourceId,
        @Param("date") LocalDate date,
        @Param("startTime") java.time.LocalTime startTime,
        @Param("endTime") java.time.LocalTime endTime,
        @Param("now") Instant now
    );

    /**
     * Find all expired payment-pending bookings that need to be marked as EXPIRED.
     * Used by scheduled job.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = 'PAYMENT_PENDING'
        AND b.lockExpiresAt <= :now
    """)
    List<Booking> findExpiredPaymentPendingBookings(@Param("now") Instant now);

    /**
     * Find user's active payment-pending booking for specific slots (for resume booking).
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.user.id = :userId
        AND b.resource.id = :resourceId
        AND b.bookingDate = :date
        AND b.status = 'PAYMENT_PENDING'
        AND b.lockExpiresAt > :now
        AND b.startTime < :endTime
        AND b.endTime > :startTime
        ORDER BY b.createdAt DESC
    """)
    java.util.Optional<Booking> findUserActivePaymentPendingBooking(
        @Param("userId") Long userId,
        @Param("resourceId") Long resourceId,
        @Param("date") LocalDate date,
        @Param("startTime") java.time.LocalTime startTime,
        @Param("endTime") java.time.LocalTime endTime,
        @Param("now") Instant now
    );

    /**
     * Update booking status to CONFIRMED (for manual payment confirmation).
     */
    @Query("""
        UPDATE Booking b
        SET b.status = 'CONFIRMED', b.lockExpiresAt = NULL
        WHERE b.reference = :reference
        AND b.status = 'PAYMENT_PENDING'
    """)
    @org.springframework.data.jpa.repository.Modifying
    int confirmPaymentPendingBooking(@Param("reference") String reference);

    /**
     * Find booking by Razorpay order ID
     */
    java.util.Optional<Booking> findByRazorpayOrderId(String razorpayOrderId);

    // ==================== PAYMENT CONFLICT DETECTION METHODS ====================

    /**
     * Find overlapping bookings for a slot where payment is already IN_PROGRESS or SUCCESS.
     * Used during Razorpay order creation to prevent multiple users from paying for the same slot.
     *
     * Returns bookings that are "locked" by payment - cannot allow another user to initiate payment.
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.resource.id = :resourceId 
        AND b.bookingDate = :date 
        AND b.startTime < :endTime AND b.endTime > :startTime 
        AND b.id != :excludeBookingId
        AND (
            b.status = 'CONFIRMED' 
            OR (
                b.status IN ('PENDING', 'AWAITING_CONFIRMATION') 
                AND b.paymentStatusEnum IN ('IN_PROGRESS', 'SUCCESS')
            )
        )
        """)
    List<Booking> findConflictingPaymentLockedBookings(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime,
            @Param("excludeBookingId") Long excludeBookingId
    );

    /**
     * Find all pending bookings for a slot with payment_status = NOT_STARTED.
     * These bookings should be marked as EXPIRED when another user successfully pays for the same slot.
     *
     * CRITICAL: Excludes the booking that just got confirmed (to avoid expiring the winner).
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.resource.id = :resourceId 
        AND b.bookingDate = :date 
        AND b.startTime < :endTime AND b.endTime > :startTime 
        AND b.id != :excludeBookingId
        AND b.status = 'PENDING'
        AND b.paymentStatusEnum = 'NOT_STARTED'
        """)
    List<Booking> findPendingBookingsToExpire(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime,
            @Param("excludeBookingId") Long excludeBookingId
    );

    /**
     * Find all abandoned bookings for a slot (PENDING with NOT_STARTED payment)
     * that should be expired when another user's payment goes IN_PROGRESS.
     *
     * CRITICAL: Also expires AWAITING_CONFIRMATION with FAILED payment status.
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.resource.id = :resourceId 
        AND b.bookingDate = :date 
        AND b.startTime < :endTime AND b.endTime > :startTime 
        AND b.id != :excludeBookingId
        AND (
            (b.status = 'PENDING' AND b.paymentStatusEnum = 'NOT_STARTED')
            OR (b.status = 'AWAITING_CONFIRMATION' AND b.paymentStatusEnum = 'FAILED')
        )
        """)
    List<Booking> findAbandonedBookingsForSlot(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime,
            @Param("excludeBookingId") Long excludeBookingId
    );

    /**
     * Bulk update abandoned bookings to EXPIRED status.
     * Used when a user successfully pays for a slot, all other pending bookings for that slot must be expired.
     */
    @Modifying
    @Query("""
        UPDATE Booking b 
        SET b.status = 'EXPIRED', b.paymentStatusEnum = 'NOT_STARTED'
        WHERE b.id IN :bookingIds
        """)
    int expireAbandonedBookings(@Param("bookingIds") List<Long> bookingIds);
}
