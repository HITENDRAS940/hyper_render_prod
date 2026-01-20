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
}

