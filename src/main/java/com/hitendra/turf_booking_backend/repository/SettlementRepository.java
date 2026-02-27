package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Settlement;
import com.hitendra.turf_booking_backend.entity.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByAdminIdOrderByCreatedAtDesc(Long adminId);

    List<Settlement> findByAdminIdAndStatusOrderByCreatedAtDesc(Long adminId, SettlementStatus status);

    Page<Settlement> findByAdminIdOrderByCreatedAtDesc(Long adminId, Pageable pageable);

    /** Paginated settlements for a specific admin within a date range. */
    @Query("SELECT s FROM Settlement s WHERE s.admin.id = :adminId " +
           "AND s.createdAt >= :from AND s.createdAt <= :to " +
           "ORDER BY s.createdAt DESC")
    Page<Settlement> findByAdminIdAndDateRange(
            @Param("adminId") Long adminId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    /** Paginated settlements across ALL admins (manager global ledger), newest first. */
    @Query("SELECT s FROM Settlement s ORDER BY s.createdAt DESC")
    Page<Settlement> findAllOrderByCreatedAtDesc(Pageable pageable);

    /** Paginated settlements across ALL admins within a date range. */
    @Query("SELECT s FROM Settlement s WHERE s.createdAt >= :from AND s.createdAt <= :to " +
           "ORDER BY s.createdAt DESC")
    Page<Settlement> findAllByDateRange(
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    /** Paginated settlements for a specific admin filtered by status. */
    Page<Settlement> findByAdminIdAndStatusOrderByCreatedAtDesc(Long adminId, SettlementStatus status, Pageable pageable);
}

