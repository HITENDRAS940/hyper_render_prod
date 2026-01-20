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

    List<Expense> findByServiceIdOrderByExpenseDateDesc(Long serviceId);

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
}

