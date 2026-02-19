package com.hitendra.turf_booking_backend.repository.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // ==================== NEW LEDGER METHODS ====================

    List<Expense> findByServiceIdOrderByExpenseDateDesc(Long serviceId);

    List<Expense> findByServiceIdAndExpenseDateBetweenOrderByExpenseDateDesc(Long serviceId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.service.id = :serviceId AND e.paymentMode = :paymentMode")
    BigDecimal sumAmountByServiceIdAndPaymentMode(@Param("serviceId") Long serviceId, @Param("paymentMode") com.hitendra.turf_booking_backend.entity.accounting.PaymentMode paymentMode);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.service.id = :serviceId")
    BigDecimal sumAmountByServiceId(@Param("serviceId") Long serviceId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.service.id = :serviceId AND e.category = :category AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByServiceIdAndCategoryAndDateRange(@Param("serviceId") Long serviceId, @Param("category") String category, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // ==================== LEGACY ADAPTER METHODS (Fixing Compilation Errors) ====================

    /**
     * Legacy support: Map 'serviceId' to 'service.id'.
     * 'category' is now a String, so eager fetch of category entity is no longer needed/possible.
     */
    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.service WHERE e.service.id = :serviceId ORDER BY e.expenseDate DESC")
    List<Expense> findByServiceIdWithRelationships(@Param("serviceId") Long serviceId);

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.service WHERE e.service.id = :serviceId AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    List<Expense> findByServiceIdAndDateRangeWithRelationships(@Param("serviceId") Long serviceId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0.0) FROM Expense e WHERE e.service.id = :serviceId AND e.expenseDate BETWEEN :startDate AND :endDate")
    Double getTotalExpensesByServiceAndDateRange(@Param("serviceId") Long serviceId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Adapted for String category. Returns category string name and amount.
     */
    @Query("""
        SELECT e.category, COALESCE(SUM(e.amount), 0.0)
        FROM Expense e
        WHERE e.service.id = :serviceId
        AND e.expenseDate BETWEEN :startDate AND :endDate
        GROUP BY e.category
        ORDER BY SUM(e.amount) DESC
    """)
    List<Object[]> getExpenseBreakdownByCategory(
        @Param("serviceId") Long serviceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}



