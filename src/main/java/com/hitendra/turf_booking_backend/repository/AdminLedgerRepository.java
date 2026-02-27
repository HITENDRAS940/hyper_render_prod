package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.AdminLedger;
import com.hitendra.turf_booking_backend.entity.AdminLedgerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AdminLedgerRepository extends JpaRepository<AdminLedger, Long> {

    /**
     * Paginated ledger entries for a specific admin + ledger type (newest first).
     */
    Page<AdminLedger> findByAdminIdAndLedgerTypeOrderByCreatedAtDesc(
            Long adminId, AdminLedgerType ledgerType, Pageable pageable);

    /**
     * Paginated ledger entries for a specific admin (both CASH + BANK combined, newest first).
     */
    Page<AdminLedger> findByAdminIdOrderByCreatedAtDesc(Long adminId, Pageable pageable);

    /**
     * Paginated ledger entries for a specific admin + ledger type within a date range (newest first).
     */
    @Query("SELECT al FROM AdminLedger al WHERE al.admin.id = :adminId " +
           "AND al.ledgerType = :ledgerType " +
           "AND al.createdAt >= :from AND al.createdAt <= :to " +
           "ORDER BY al.createdAt DESC, al.id DESC")
    Page<AdminLedger> findByAdminIdAndLedgerTypeAndDateRange(
            @Param("adminId") Long adminId,
            @Param("ledgerType") AdminLedgerType ledgerType,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    /**
     * Paginated combined (CASH + BANK) ledger entries for a specific admin within a date range.
     */
    @Query("SELECT al FROM AdminLedger al WHERE al.admin.id = :adminId " +
           "AND al.createdAt >= :from AND al.createdAt <= :to " +
           "ORDER BY al.createdAt DESC, al.id DESC")
    Page<AdminLedger> findByAdminIdAndDateRange(
            @Param("adminId") Long adminId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    /**
     * Latest entry for a specific admin + ledger type — used for running balance.
     */
    @Query("SELECT al FROM AdminLedger al WHERE al.admin.id = :adminId " +
           "AND al.ledgerType = :ledgerType ORDER BY al.createdAt DESC, al.id DESC")
    Page<AdminLedger> findLatestEntries(
            @Param("adminId") Long adminId,
            @Param("ledgerType") AdminLedgerType ledgerType,
            Pageable pageable);

    /**
     * Get the current running balance for a sub-ledger (most recent balanceAfter).
     */
    @Query("SELECT al.balanceAfter FROM AdminLedger al " +
           "WHERE al.admin.id = :adminId AND al.ledgerType = :ledgerType " +
           "ORDER BY al.createdAt DESC, al.id DESC")
    Page<java.math.BigDecimal> findCurrentBalance(
            @Param("adminId") Long adminId,
            @Param("ledgerType") AdminLedgerType ledgerType,
            Pageable pageable);
}

