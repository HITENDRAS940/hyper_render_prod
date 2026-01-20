package com.hitendra.turf_booking_backend.repository.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.CashLedger;
import com.hitendra.turf_booking_backend.entity.accounting.LedgerSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashLedgerRepository extends JpaRepository<CashLedger, Long> {

    /**
     * Get the latest ledger entry for a service (for calculating running balance).
     * Uses pessimistic lock to prevent race conditions during concurrent transactions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c FROM CashLedger c
        WHERE c.service.id = :serviceId
        ORDER BY c.createdAt DESC, c.id DESC
        LIMIT 1
    """)
    Optional<CashLedger> findLatestByServiceIdWithLock(@Param("serviceId") Long serviceId);

    /**
     * Get ledger entries for a service ordered by time.
     */
    List<CashLedger> findByServiceIdOrderByCreatedAtDesc(Long serviceId);

    /**
     * Get ledger entries for a service within a date range.
     */
    @Query("""
        SELECT c FROM CashLedger c
        WHERE c.service.id = :serviceId
        AND c.createdAt BETWEEN :startDate AND :endDate
        ORDER BY c.createdAt DESC
    """)
    List<CashLedger> findByServiceIdAndDateRange(
        @Param("serviceId") Long serviceId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Get total credits (money in) for a service in a date range.
     */
    @Query("""
        SELECT COALESCE(SUM(c.creditAmount), 0.0)
        FROM CashLedger c
        WHERE c.service.id = :serviceId
        AND c.createdAt BETWEEN :startDate AND :endDate
    """)
    Double getTotalCreditsByServiceAndDateRange(
        @Param("serviceId") Long serviceId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Get total debits (money out) for a service in a date range.
     */
    @Query("""
        SELECT COALESCE(SUM(c.debitAmount), 0.0)
        FROM CashLedger c
        WHERE c.service.id = :serviceId
        AND c.createdAt BETWEEN :startDate AND :endDate
    """)
    Double getTotalDebitsByServiceAndDateRange(
        @Param("serviceId") Long serviceId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Get ledger entries by source type.
     */
    List<CashLedger> findByServiceIdAndSourceOrderByCreatedAtDesc(Long serviceId, LedgerSource source);

    /**
     * Get current balance for a service.
     */
    @Query("""
        SELECT c.balanceAfter FROM CashLedger c
        WHERE c.service.id = :serviceId
        ORDER BY c.createdAt DESC, c.id DESC
        LIMIT 1
    """)
    Optional<Double> getCurrentBalance(@Param("serviceId") Long serviceId);
}

