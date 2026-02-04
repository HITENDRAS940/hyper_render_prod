package com.hitendra.turf_booking_backend.repository.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByServiceIdAndActiveTrue(Long serviceId);

    List<InventoryItem> findByServiceId(Long serviceId);

    Optional<InventoryItem> findByIdAndServiceId(Long id, Long serviceId);

    @Query("""
        SELECT i FROM InventoryItem i
        WHERE i.service.id = :serviceId
        AND i.active = true
        AND i.stockQuantity <= i.minStockLevel
        ORDER BY i.stockQuantity ASC
    """)
    List<InventoryItem> findLowStockItems(@Param("serviceId") Long serviceId);

    @Query("""
        SELECT i FROM InventoryItem i
        WHERE i.service.id = :serviceId
        AND i.active = true
        AND i.stockQuantity = 0
    """)
    List<InventoryItem> findOutOfStockItems(@Param("serviceId") Long serviceId);

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Count active inventory items for a service.
     */
    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.service.id = :serviceId AND i.active = true")
    long countActiveByServiceId(@Param("serviceId") Long serviceId);

    /**
     * Count low stock items for a service (for dashboard alerts).
     */
    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.service.id = :serviceId AND i.active = true AND i.stockQuantity <= i.minStockLevel")
    long countLowStockItems(@Param("serviceId") Long serviceId);

    /**
     * Count out of stock items for a service (for dashboard alerts).
     */
    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.service.id = :serviceId AND i.active = true AND i.stockQuantity = 0")
    long countOutOfStockItems(@Param("serviceId") Long serviceId);

    /**
     * Get total inventory value for a service.
     */
    @Query("SELECT COALESCE(SUM(i.stockQuantity * i.costPrice), 0.0) FROM InventoryItem i WHERE i.service.id = :serviceId AND i.active = true")
    Double getTotalInventoryValue(@Param("serviceId") Long serviceId);

    /**
     * Get only item names for a service (for dropdown).
     */
    @Query("SELECT i.name FROM InventoryItem i WHERE i.service.id = :serviceId AND i.active = true ORDER BY i.name")
    List<String> findActiveItemNames(@Param("serviceId") Long serviceId);

    // ==================== END OPTIMIZED QUERIES ====================
}

