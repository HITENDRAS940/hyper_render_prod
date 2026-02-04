package com.hitendra.turf_booking_backend.repository.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.InventorySale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventorySaleRepository extends JpaRepository<InventorySale, Long> {

    List<InventorySale> findByServiceIdOrderBySaleDateDesc(Long serviceId);

    List<InventorySale> findByServiceIdAndSaleDateBetweenOrderBySaleDateDesc(
        Long serviceId, LocalDate startDate, LocalDate endDate);

    @Query("""
        SELECT COALESCE(SUM(s.totalAmount), 0.0)
        FROM InventorySale s
        WHERE s.service.id = :serviceId
        AND s.saleDate BETWEEN :startDate AND :endDate
    """)
    Double getTotalSalesByServiceAndDateRange(
        @Param("serviceId") Long serviceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Count sales for a service in date range.
     */
    @Query("SELECT COUNT(s) FROM InventorySale s WHERE s.service.id = :serviceId AND s.saleDate BETWEEN :startDate AND :endDate")
    long countByServiceIdAndDateRange(
        @Param("serviceId") Long serviceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get sale IDs for batch operations.
     */
    @Query("SELECT s.id FROM InventorySale s WHERE s.service.id = :serviceId ORDER BY s.saleDate DESC")
    List<Long> findSaleIdsByServiceId(@Param("serviceId") Long serviceId);

    /**
     * Get total quantity sold for an item (for analytics).
     * Joins through InventorySaleItem to sum quantities.
     */
    @Query("SELECT COALESCE(SUM(si.quantity), 0) FROM InventorySaleItem si JOIN si.sale s WHERE si.item.id = :itemId AND s.saleDate BETWEEN :startDate AND :endDate")
    Long getTotalQuantitySoldByItemAndDateRange(
        @Param("itemId") Long itemId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // ==================== END OPTIMIZED QUERIES ====================
}

