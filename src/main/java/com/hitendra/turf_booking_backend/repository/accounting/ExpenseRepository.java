package com.hitendra.turf_booking_backend.repository.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // ==================== EAGER FETCH QUERIES (Prevent LazyInitializationException) ====================

    /**
     * Get expenses by service with eager loading of service and category.
     * Prevents LazyInitializationException when accessing service.name or category.name.
     */
    @Query("""
        SELECT e FROM Expense e
        LEFT JOIN FETCH e.service s
        LEFT JOIN FETCH e.category c
        WHERE e.service.id = :serviceId
        ORDER BY e.expenseDate DESC
        """)
    List<Expense> findByServiceIdWithRelationships(@Param("serviceId") Long serviceId);

    /**
     * Get expenses by service and date range with eager loading.
     */
    @Query("""
        SELECT e FROM Expense e
        LEFT JOIN FETCH e.service s
        LEFT JOIN FETCH e.category c
        WHERE e.service.id = :serviceId
        AND e.expenseDate BETWEEN :startDate AND :endDate
        ORDER BY e.expenseDate DESC
        """)
    List<Expense> findByServiceIdAndDateRangeWithRelationships(
        @Param("serviceId") Long serviceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // ==================== LEGACY METHODS (Deprecated - use eager fetch versions) ====================

    @Deprecated
    List<Expense> findByServiceIdOrderByExpenseDateDesc(Long serviceId);

    @Deprecated
    List<Expense> findByServiceIdAndExpenseDateBetweenOrderByExpenseDateDesc(
        Long serviceId, LocalDate startDate, LocalDate endDate);

    List<Expense> findByCategoryIdAndServiceId(Long categoryId, Long serviceId);

    @Query("""
        SELECT e FROM Expense e
        WHERE e.service.id = :serviceId
        AND e.category.id = :categoryId
        AND e.expenseDate BETWEEN :startDate AND :endDate
        ORDER BY e.expenseDate DESC
    """)
    List<Expense> findByCategoryAndDateRange(
        @Param("serviceId") Long serviceId,
        @Param("categoryId") Long categoryId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0.0)
        FROM Expense e
        WHERE e.service.id = :serviceId
        AND e.expenseDate BETWEEN :startDate AND :endDate
    """)
    Double getTotalExpensesByServiceAndDateRange(
        @Param("serviceId") Long serviceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT e.category.name, COALESCE(SUM(e.amount), 0.0)
        FROM Expense e
        WHERE e.service.id = :serviceId
        AND e.expenseDate BETWEEN :startDate AND :endDate
        GROUP BY e.category.id, e.category.name
        ORDER BY SUM(e.amount) DESC
    """)
    List<Object[]> getExpenseBreakdownByCategory(
        @Param("serviceId") Long serviceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Count expenses for a service (for pagination info).
     */
    @Query("SELECT COUNT(e) FROM Expense e WHERE e.service.id = :serviceId")
    long countByServiceId(@Param("serviceId") Long serviceId);

    /**
     * Count expenses for a service in date range.
     */
    @Query("SELECT COUNT(e) FROM Expense e WHERE e.service.id = :serviceId AND e.expenseDate BETWEEN :startDate AND :endDate")
    long countByServiceIdAndDateRange(
        @Param("serviceId") Long serviceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get expense IDs for a service in date range (for batch operations).
     */
    @Query("SELECT e.id FROM Expense e WHERE e.service.id = :serviceId AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    List<Long> findExpenseIdsByServiceIdAndDateRange(
        @Param("serviceId") Long serviceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // ==================== END OPTIMIZED QUERIES ====================
}

