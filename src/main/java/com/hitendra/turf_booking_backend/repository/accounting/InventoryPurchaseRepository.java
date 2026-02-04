package com.hitendra.turf_booking_backend.repository.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.InventoryPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryPurchaseRepository extends JpaRepository<InventoryPurchase, Long> {

    List<InventoryPurchase> findByServiceIdOrderByPurchaseDateDesc(Long serviceId);

    List<InventoryPurchase> findByServiceIdAndPurchaseDateBetweenOrderByPurchaseDateDesc(
        Long serviceId, LocalDate startDate, LocalDate endDate);

    @Query("""
        SELECT COALESCE(SUM(ip.totalAmount), 0.0)
        FROM InventoryPurchase ip
        WHERE ip.service.id = :serviceId
        AND ip.purchaseDate BETWEEN :startDate AND :endDate
    """)
    Double getTotalPurchasesByServiceAndDateRange(
        @Param("serviceId") Long serviceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Count purchases for a service in date range.
     */
    @Query("SELECT COUNT(ip) FROM InventoryPurchase ip WHERE ip.service.id = :serviceId AND ip.purchaseDate BETWEEN :startDate AND :endDate")
    long countByServiceIdAndDateRange(
        @Param("serviceId") Long serviceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get purchase IDs for batch operations.
     */
    @Query("SELECT ip.id FROM InventoryPurchase ip WHERE ip.service.id = :serviceId ORDER BY ip.purchaseDate DESC")
    List<Long> findPurchaseIdsByServiceId(@Param("serviceId") Long serviceId);

    // ==================== END OPTIMIZED QUERIES ====================
}

