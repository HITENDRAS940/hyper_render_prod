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

    List<Expense> findByAdminProfileIdOrderByExpenseDateDesc(Long adminProfileId);

    List<Expense> findByAdminProfileIdAndExpenseDateBetweenOrderByExpenseDateDesc(
            Long adminProfileId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.adminProfile.id = :adminProfileId AND e.paymentMode = :paymentMode")
    BigDecimal sumAmountByAdminProfileIdAndPaymentMode(
            @Param("adminProfileId") Long adminProfileId,
            @Param("paymentMode") com.hitendra.turf_booking_backend.entity.accounting.ExpensePaymentMode paymentMode);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.adminProfile.id = :adminProfileId")
    BigDecimal sumAmountByAdminProfileId(@Param("adminProfileId") Long adminProfileId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.adminProfile.id = :adminProfileId AND e.category = :category AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByAdminProfileIdAndCategoryAndDateRange(
            @Param("adminProfileId") Long adminProfileId,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.adminProfile WHERE e.adminProfile.id = :adminProfileId ORDER BY e.expenseDate DESC")
    List<Expense> findByAdminProfileIdWithRelationships(@Param("adminProfileId") Long adminProfileId);

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.adminProfile WHERE e.adminProfile.id = :adminProfileId AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    List<Expense> findByAdminProfileIdAndDateRangeWithRelationships(
            @Param("adminProfileId") Long adminProfileId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0.0) FROM Expense e WHERE e.adminProfile.id = :adminProfileId AND e.expenseDate BETWEEN :startDate AND :endDate")
    Double getTotalExpensesByAdminProfileIdAndDateRange(
            @Param("adminProfileId") Long adminProfileId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT e.category, COALESCE(SUM(e.amount), 0.0)
        FROM Expense e
        WHERE e.adminProfile.id = :adminProfileId
        AND e.expenseDate BETWEEN :startDate AND :endDate
        GROUP BY e.category
        ORDER BY SUM(e.amount) DESC
    """)
    List<Object[]> getExpenseBreakdownByCategory(
            @Param("adminProfileId") Long adminProfileId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ==================== LEGACY SERVICE-BASED METHODS (kept for ServiceFinancialService) ====================

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.adminProfile WHERE e.adminProfile.id IN (SELECT ap.id FROM AdminProfile ap JOIN ap.managedServices s WHERE s.id = :serviceId) ORDER BY e.expenseDate DESC")
    List<Expense> findByServiceIdOrderByExpenseDateDesc(@Param("serviceId") Long serviceId);

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.adminProfile WHERE e.adminProfile.id IN (SELECT ap.id FROM AdminProfile ap JOIN ap.managedServices s WHERE s.id = :serviceId) AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    List<Expense> findByServiceIdAndExpenseDateBetweenOrderByExpenseDateDesc(
            @Param("serviceId") Long serviceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}



