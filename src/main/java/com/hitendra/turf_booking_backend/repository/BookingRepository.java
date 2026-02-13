package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Booking;
import com.hitendra.turf_booking_backend.entity.BookingStatus;
import com.hitendra.turf_booking_backend.entity.PaymentStatus;
import com.hitendra.turf_booking_backend.entity.ServiceResource;
import com.hitendra.turf_booking_backend.repository.projection.BookingListProjection;
import com.hitendra.turf_booking_backend.repository.projection.UserBookingProjection;
import com.hitendra.turf_booking_backend.repository.projection.BookingCountProjection;
import com.hitendra.turf_booking_backend.repository.projection.SlotOverlapProjection;
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

    /**
     * Get top 50 bookings with eager loading for debug purposes.
     * Uses JOIN FETCH to avoid LazyInitializationException.
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.user
        LEFT JOIN FETCH b.adminProfile
        LEFT JOIN FETCH b.service s
        LEFT JOIN FETCH s.createdBy
        ORDER BY b.createdAt DESC
        """)
    List<Booking> findTop50ByOrderByCreatedAtDesc();

    // ==================== OPTIMIZED PROJECTION QUERIES ====================

    /**
     * Get lightweight booking list for a service (projection-based).
     * Avoids loading full entities and their relationships.
     */
    @Query("""
        SELECT b.id as id, b.reference as reference, b.bookingDate as bookingDate,
               b.startTime as startTime, b.endTime as endTime, b.amount as amount,
               CAST(b.status AS string) as status, b.createdAt as createdAt,
               b.onlineAmountPaid as onlineAmountPaid, b.venueAmountDue as venueAmountDue,
               b.venueAmountCollected as venueAmountCollected,
               CAST(b.paymentStatusEnum AS string) as paymentStatus,
               b.service.id as serviceId, b.service.name as serviceName,
               b.resource.id as resourceId, b.resource.name as resourceName,
               b.user.id as userId, b.user.name as userName, 
               b.user.email as userEmail, b.user.phone as userPhone
        FROM Booking b
        WHERE b.service.id = :serviceId
        ORDER BY b.createdAt DESC
        """)
    Page<BookingListProjection> findBookingsByServiceIdProjected(@Param("serviceId") Long serviceId, Pageable pageable);

    /**
     * Get lightweight booking list for a user (projection-based).
     */
    @Query("""
        SELECT b.id as id, b.reference as reference, CAST(b.status AS string) as status, b.bookingDate as bookingDate,
               b.startTime as startTime, b.endTime as endTime, b.amount as amount,
               b.createdAt as createdAt,
               b.onlineAmountPaid as onlineAmountPaid, b.venueAmountDue as venueAmountDue,
               b.service.id as serviceId, b.service.name as serviceName,
               b.resource.id as resourceId, b.resource.name as resourceName
        FROM Booking b
        WHERE b.user.id = :userId
        ORDER BY b.createdAt DESC
        """)
    List<UserBookingProjection> findUserBookingsProjected(@Param("userId") Long userId);

    /**
     * Get paginated lightweight booking list for a user (projection-based).
     */
    @Query("""
        SELECT b.id as id, b.reference as reference, CAST(b.status AS string) as status, b.bookingDate as bookingDate,
               b.startTime as startTime, b.endTime as endTime, b.amount as amount,
               b.createdAt as createdAt,
               b.onlineAmountPaid as onlineAmountPaid, b.venueAmountDue as venueAmountDue,
               b.service.id as serviceId, b.service.name as serviceName,
               b.resource.id as resourceId, b.resource.name as resourceName
        FROM Booking b
        WHERE b.user.id = :userId
        ORDER BY b.createdAt DESC
        """)
    Page<UserBookingProjection> findUserBookingsProjectedPaged(@Param("userId") Long userId, Pageable pageable);

    /**
     * Get last booking for a user (optimized - only fetches one record).
     */
    @Query("""
        SELECT b.id as id, CAST(b.status AS string) as status, b.bookingDate as bookingDate,
               b.startTime as startTime, b.endTime as endTime, b.amount as amount,
               b.createdAt as createdAt,
               b.service.id as serviceId, b.service.name as serviceName,
               b.resource.id as resourceId, b.resource.name as resourceName
        FROM Booking b
        WHERE b.user.id = :userId
        ORDER BY b.createdAt DESC
        LIMIT 1
        """)
    UserBookingProjection findLastUserBookingProjected(@Param("userId") Long userId);

    /**
     * Get booking counts by status for a user (for statistics).
     */
    @Query("""
        SELECT CAST(b.status AS string) as status, COUNT(b) as count
        FROM Booking b
        WHERE b.user.id = :userId
        GROUP BY b.status
        """)
    List<BookingCountProjection> getBookingCountsByStatusForUser(@Param("userId") Long userId);

    /**
     * Get lightweight booking list for admin (projection-based).
     * Includes both user bookings for admin's services and manual bookings created by admin.
     */
    @Query("""
        SELECT b.id as id, b.reference as reference, b.bookingDate as bookingDate,
               b.startTime as startTime, b.endTime as endTime, b.amount as amount,
               CAST(b.status AS string) as status, b.createdAt as createdAt,
               b.onlineAmountPaid as onlineAmountPaid, b.venueAmountDue as venueAmountDue,
               b.venueAmountCollected as venueAmountCollected,
               CAST(b.paymentStatusEnum AS string) as paymentStatus,
               s.id as serviceId, s.name as serviceName,
               r.id as resourceId, r.name as resourceName,
               u.id as userId, u.name as userName, 
               u.email as userEmail, u.phone as userPhone
        FROM Booking b
        LEFT JOIN b.service s
        LEFT JOIN s.createdBy sc
        LEFT JOIN b.adminProfile ap
        LEFT JOIN b.resource r
        LEFT JOIN b.user u
        WHERE sc.id = :adminId OR ap.id = :adminId
        ORDER BY b.createdAt DESC
        """)
    Page<BookingListProjection> findBookingsByAdminIdProjected(@Param("adminId") Long adminId, Pageable pageable);

    /**
     * Get lightweight booking list for admin filtered by date (projection-based).
     * Includes both user bookings for admin's services and manual bookings created by admin.
     */
    @Query("""
        SELECT b.id as id, b.reference as reference, b.bookingDate as bookingDate,
               b.startTime as startTime, b.endTime as endTime, b.amount as amount,
               CAST(b.status AS string) as status, b.createdAt as createdAt,
               b.onlineAmountPaid as onlineAmountPaid, b.venueAmountDue as venueAmountDue,
               b.venueAmountCollected as venueAmountCollected,
               CAST(b.paymentStatusEnum AS string) as paymentStatus,
               s.id as serviceId, s.name as serviceName,
               r.id as resourceId, r.name as resourceName,
               u.id as userId, u.name as userName, 
               u.email as userEmail, u.phone as userPhone
        FROM Booking b
        LEFT JOIN b.service s
        LEFT JOIN s.createdBy sc
        LEFT JOIN b.adminProfile ap
        LEFT JOIN b.resource r
        LEFT JOIN b.user u
        WHERE (sc.id = :adminId OR ap.id = :adminId) AND b.bookingDate = :date
        ORDER BY b.createdAt DESC
        """)
    Page<BookingListProjection> findBookingsByAdminIdAndDateProjected(
            @Param("adminId") Long adminId, @Param("date") LocalDate date, Pageable pageable);

    /**
     * Get lightweight booking list for admin filtered by status (projection-based).
     * Includes both user bookings for admin's services and manual bookings created by admin.
     */
    @Query("""
        SELECT b.id as id, b.reference as reference, b.bookingDate as bookingDate,
               b.startTime as startTime, b.endTime as endTime, b.amount as amount,
               CAST(b.status AS string) as status, b.createdAt as createdAt,
               b.onlineAmountPaid as onlineAmountPaid, b.venueAmountDue as venueAmountDue,
               b.venueAmountCollected as venueAmountCollected,
               CAST(b.paymentStatusEnum AS string) as paymentStatus,
               s.id as serviceId, s.name as serviceName,
               r.id as resourceId, r.name as resourceName,
               u.id as userId, u.name as userName, 
               u.email as userEmail, u.phone as userPhone
        FROM Booking b
        LEFT JOIN b.service s
        LEFT JOIN s.createdBy sc
        LEFT JOIN b.adminProfile ap
        LEFT JOIN b.resource r
        LEFT JOIN b.user u
        WHERE (sc.id = :adminId OR ap.id = :adminId) AND b.status = :status
        ORDER BY b.createdAt DESC
        """)
    Page<BookingListProjection> findBookingsByAdminIdAndStatusProjected(
            @Param("adminId") Long adminId, @Param("status") BookingStatus status, Pageable pageable);

    /**
     * Get lightweight booking list for admin filtered by date and status (projection-based).
     * Includes both user bookings for admin's services and manual bookings created by admin.
     */
    @Query("""
        SELECT b.id as id, b.reference as reference, b.bookingDate as bookingDate,
               b.startTime as startTime, b.endTime as endTime, b.amount as amount,
               CAST(b.status AS string) as status, b.createdAt as createdAt,
               b.onlineAmountPaid as onlineAmountPaid, b.venueAmountDue as venueAmountDue,
               b.venueAmountCollected as venueAmountCollected,
               CAST(b.paymentStatusEnum AS string) as paymentStatus,
               s.id as serviceId, s.name as serviceName,
               r.id as resourceId, r.name as resourceName,
               u.id as userId, u.name as userName, 
               u.email as userEmail, u.phone as userPhone
        FROM Booking b
        LEFT JOIN b.service s
        LEFT JOIN s.createdBy sc
        LEFT JOIN b.adminProfile ap
        LEFT JOIN b.resource r
        LEFT JOIN b.user u
        WHERE (sc.id = :adminId OR ap.id = :adminId) AND b.bookingDate = :date AND b.status = :status
        ORDER BY b.createdAt DESC
        """)
    Page<BookingListProjection> findBookingsByAdminIdAndDateAndStatusProjected(
            @Param("adminId") Long adminId, @Param("date") LocalDate date,
            @Param("status") BookingStatus status, Pageable pageable);

    /**
     * Get lightweight booking list by status (projection-based).
     */
    @Query("""
        SELECT b.id as id, b.reference as reference, b.bookingDate as bookingDate,
               b.startTime as startTime, b.endTime as endTime, b.amount as amount,
               CAST(b.status AS string) as status, b.createdAt as createdAt,
               b.onlineAmountPaid as onlineAmountPaid, b.venueAmountDue as venueAmountDue,
               b.venueAmountCollected as venueAmountCollected,
               CAST(b.paymentStatusEnum AS string) as paymentStatus,
               b.service.id as serviceId, b.service.name as serviceName,
               b.resource.id as resourceId, b.resource.name as resourceName,
               b.user.id as userId, b.user.name as userName, 
               b.user.email as userEmail, b.user.phone as userPhone
        FROM Booking b
        WHERE b.status = :status
        ORDER BY b.createdAt DESC
        """)
    Page<BookingListProjection> findBookingsByStatusProjected(@Param("status") BookingStatus status, Pageable pageable);

    /**
     * Get lightweight booking list by resource (projection-based).
     */
    @Query("""
        SELECT b.id as id, b.reference as reference, b.bookingDate as bookingDate,
               b.startTime as startTime, b.endTime as endTime, b.amount as amount,
               CAST(b.status AS string) as status, b.createdAt as createdAt,
               b.onlineAmountPaid as onlineAmountPaid, b.venueAmountDue as venueAmountDue,
               b.venueAmountCollected as venueAmountCollected,
               CAST(b.paymentStatusEnum AS string) as paymentStatus,
               b.service.id as serviceId, b.service.name as serviceName,
               b.resource.id as resourceId, b.resource.name as resourceName,
               b.user.id as userId, b.user.name as userName, 
               b.user.email as userEmail, b.user.phone as userPhone
        FROM Booking b
        WHERE b.resource.id = :resourceId
        ORDER BY b.createdAt DESC
        """)
    Page<BookingListProjection> findBookingsByResourceIdProjected(@Param("resourceId") Long resourceId, Pageable pageable);

    /**
     * Get lightweight booking list by resource and date (projection-based).
     */
    @Query("""
        SELECT b.id as id, b.reference as reference, b.bookingDate as bookingDate,
               b.startTime as startTime, b.endTime as endTime, b.amount as amount,
               CAST(b.status AS string) as status, b.createdAt as createdAt,
               b.onlineAmountPaid as onlineAmountPaid, b.venueAmountDue as venueAmountDue,
               b.venueAmountCollected as venueAmountCollected,
               CAST(b.paymentStatusEnum AS string) as paymentStatus,
               b.service.id as serviceId, b.service.name as serviceName,
               b.resource.id as resourceId, b.resource.name as resourceName,
               b.user.id as userId, b.user.name as userName, 
               b.user.email as userEmail, b.user.phone as userPhone
        FROM Booking b
        WHERE b.resource.id = :resourceId AND b.bookingDate = :date
        ORDER BY b.createdAt DESC
        """)
    Page<BookingListProjection> findBookingsByResourceIdAndDateProjected(
            @Param("resourceId") Long resourceId, @Param("date") LocalDate date, Pageable pageable);

    /**
     * Optimized overlap check - returns only IDs and essential fields.
     * Used for fast availability checking without loading full entities.
     */
    @Query("""
        SELECT b.id as id, b.resource.id as resourceId, b.bookingDate as bookingDate,
               b.startTime as startTime, b.endTime as endTime,
               CAST(b.status AS string) as status, CAST(b.paymentStatusEnum AS string) as paymentStatus
        FROM Booking b 
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
    List<SlotOverlapProjection> findOverlappingBookingsProjected(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime
    );

    /**
     * Check if any overlapping booking exists (returns boolean for fast check).
     */
    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM Booking b 
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
    boolean existsOverlappingBooking(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime
    );

    // ==================== END OPTIMIZED PROJECTION QUERIES ====================

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
     * Find all expired awaiting-confirmation bookings that need to be marked as EXPIRED.
     * Used by PaymentTimeoutScheduler to efficiently query bookings without loading all records.
     * OPTIMIZED: Database-level filtering instead of loading all bookings into memory.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = 'AWAITING_CONFIRMATION'
        AND b.lockExpiresAt IS NOT NULL
        AND b.lockExpiresAt < :now
    """)
    List<Booking> findExpiredAwaitingConfirmationBookings(@Param("now") Instant now);

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

    /**
     * Find all bookings for services created by a specific admin or manually created by this admin
     * Includes both user bookings for admin's services and manual bookings created by admin
     * Used for admin dashboard
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE (b.service.createdBy.id = :adminId OR b.adminProfile.id = :adminId) " +
           "ORDER BY b.createdAt DESC")
    Page<Booking> findByServiceCreatedById(
            @Param("adminId") Long adminId,
            Pageable pageable
    );

    /**
     * Find all bookings for services created by a specific admin or manually created by this admin filtered by date
     * Includes both user bookings for admin's services and manual bookings created by admin
     * Used for admin dashboard with date filtering
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE (b.service.createdBy.id = :adminId OR b.adminProfile.id = :adminId) " +
           "AND b.bookingDate = :date " +
           "ORDER BY b.createdAt DESC")
    Page<Booking> findByServiceCreatedByIdAndDate(
            @Param("adminId") Long adminId,
            @Param("date") LocalDate date,
            Pageable pageable
    );

    /**
     * Find all bookings for services created by a specific admin or manually created by this admin filtered by status
     * Includes both user bookings for admin's services and manual bookings created by admin
     * Used for admin dashboard with status filtering
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE (b.service.createdBy.id = :adminId OR b.adminProfile.id = :adminId) " +
           "AND b.status = :status " +
           "ORDER BY b.createdAt DESC")
    Page<Booking> findByServiceCreatedByIdAndStatus(
            @Param("adminId") Long adminId,
            @Param("status") BookingStatus status,
            Pageable pageable
    );

    /**
     * Find all bookings for services created by a specific admin or manually created by this admin filtered by date and status
     * Includes both user bookings for admin's services and manual bookings created by admin
     * Used for admin dashboard with both filters
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE (b.service.createdBy.id = :adminId OR b.adminProfile.id = :adminId) " +
           "AND b.bookingDate = :date " +
           "AND b.status = :status " +
           "ORDER BY b.createdAt DESC")
    Page<Booking> findByServiceCreatedByIdAndDateAndStatus(
            @Param("adminId") Long adminId,
            @Param("date") LocalDate date,
            @Param("status") BookingStatus status,
            Pageable pageable
    );

    /**
     * Find all pending bookings for services created by a specific admin or manually created by this admin
     * Includes both user bookings for admin's services and manual bookings created by admin
     * Used for admin dashboard
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE (b.service.createdBy.id = :adminId OR b.adminProfile.id = :adminId) " +
           "AND b.status = 'PENDING' " +
           "ORDER BY b.createdAt DESC")
    Page<Booking> findPendingByServiceCreatedById(
            @Param("adminId") Long adminId,
            Pageable pageable
    );

    // ==================== GDPR/PRIVACY COMPLIANCE METHODS ====================

    /**
     * Unlink all bookings from a user for GDPR-compliant account deletion.
     * Sets user_id = NULL for all bookings belonging to this user.
     * This preserves booking records for business purposes while removing
     * any association with the deleted user.
     *
     * @param userId The user ID whose bookings should be unlinked
     * @return Number of bookings unlinked
     */
    @Modifying
    @Query("UPDATE Booking b SET b.user = NULL WHERE b.user.id = :userId")
    int unlinkBookingsFromUser(@Param("userId") Long userId);

    /**
     * Unlink all bookings created by an admin for GDPR-compliant account deletion.
     * Sets created_by_admin_id = NULL for all bookings created by this admin.
     *
     * @param adminProfileId The admin profile ID whose created bookings should be unlinked
     * @return Number of bookings unlinked
     */
    @Modifying
    @Query("UPDATE Booking b SET b.adminProfile = NULL WHERE b.adminProfile.id = :adminProfileId")
    int unlinkBookingsFromAdmin(@Param("adminProfileId") Long adminProfileId);

    // ==================== ADMIN DASHBOARD STATISTICS ====================

    /**
     * Count online bookings for admin's services on a specific date
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.service.createdBy.id = :adminId
        AND b.bookingDate = :date
        AND b.status IN ('CONFIRMED', 'COMPLETED')
        AND b.paymentSource IN ('BY_USER', 'ONLINE', 'WALLET_PLUS_ONLINE')
        """)
    Long countOnlineBookingsByAdminAndDate(
            @Param("adminId") Long adminId,
            @Param("date") LocalDate date
    );

    /**
     * Count offline bookings for admin's services on a specific date
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.service.createdBy.id = :adminId
        AND b.bookingDate = :date
        AND b.status IN ('CONFIRMED', 'COMPLETED')
        AND b.paymentSource = 'BY_ADMIN'
        """)
    Long countOfflineBookingsByAdminAndDate(
            @Param("adminId") Long adminId,
            @Param("date") LocalDate date
    );

    /**
     * Sum online revenue for admin's services on a specific date
     */
    @Query("""
        SELECT COALESCE(SUM(b.amount), 0) FROM Booking b
        WHERE b.service.createdBy.id = :adminId
        AND b.bookingDate = :date
        AND b.status IN ('CONFIRMED', 'COMPLETED')
        AND b.paymentSource IN ('BY_USER', 'ONLINE', 'WALLET_PLUS_ONLINE')
        """)
    Double sumOnlineRevenueByAdminAndDate(
            @Param("adminId") Long adminId,
            @Param("date") LocalDate date
    );

    /**
     * Sum offline revenue for admin's services on a specific date
     */
    @Query("""
        SELECT COALESCE(SUM(b.amount), 0) FROM Booking b
        WHERE b.service.createdBy.id = :adminId
        AND b.bookingDate = :date
        AND b.status IN ('CONFIRMED', 'COMPLETED')
        AND b.paymentSource = 'BY_ADMIN'
        """)
    Double sumOfflineRevenueByAdminAndDate(
            @Param("adminId") Long adminId,
            @Param("date") LocalDate date
    );

    /**
     * Count online bookings for admin's services in a date range
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.service.createdBy.id = :adminId
        AND b.bookingDate BETWEEN :startDate AND :endDate
        AND b.status IN ('CONFIRMED', 'COMPLETED')
        AND b.paymentSource IN ('BY_USER', 'ONLINE', 'WALLET_PLUS_ONLINE')
        """)
    Long countOnlineBookingsByAdminAndDateRange(
            @Param("adminId") Long adminId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Count offline bookings for admin's services in a date range
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.service.createdBy.id = :adminId
        AND b.bookingDate BETWEEN :startDate AND :endDate
        AND b.status IN ('CONFIRMED', 'COMPLETED')
        AND b.paymentSource = 'BY_ADMIN'
        """)
    Long countOfflineBookingsByAdminAndDateRange(
            @Param("adminId") Long adminId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Sum online revenue for admin's services in a date range
     */
    @Query("""
        SELECT COALESCE(SUM(b.amount), 0) FROM Booking b
        WHERE b.service.createdBy.id = :adminId
        AND b.bookingDate BETWEEN :startDate AND :endDate
        AND b.status IN ('CONFIRMED', 'COMPLETED')
        AND b.paymentSource IN ('BY_USER', 'ONLINE', 'WALLET_PLUS_ONLINE')
        """)
    Double sumOnlineRevenueByAdminAndDateRange(
            @Param("adminId") Long adminId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Sum offline revenue for admin's services in a date range
     */
    @Query("""
        SELECT COALESCE(SUM(b.amount), 0) FROM Booking b
        WHERE b.service.createdBy.id = :adminId
        AND b.bookingDate BETWEEN :startDate AND :endDate
        AND b.status IN ('CONFIRMED', 'COMPLETED')
        AND b.paymentSource = 'BY_ADMIN'
        """)
    Double sumOfflineRevenueByAdminAndDateRange(
            @Param("adminId") Long adminId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
