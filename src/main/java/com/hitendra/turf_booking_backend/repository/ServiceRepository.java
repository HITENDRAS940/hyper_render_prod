package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Service;
import com.hitendra.turf_booking_backend.repository.projection.ServiceCardProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    @Modifying
    @Transactional
    void deleteServiceById(Long id);

    List<Service> findByCreatedById(Long adminProfileId);
    Page<Service> findByCreatedById(Long adminProfileId, Pageable pageable);

    @Query("SELECT new com.hitendra.turf_booking_backend.dto.service.AdminServiceSummaryDto(" +
            "s.id, s.name, s.location, s.city, s.availability) " +
            "FROM Service s WHERE s.createdBy.id = :adminProfileId")
    Page<com.hitendra.turf_booking_backend.dto.service.AdminServiceSummaryDto> findAdminServiceSummaryByCreatedById(
            @Param("adminProfileId") Long adminProfileId, Pageable pageable);
    List<Service> findByCityIgnoreCase(String city);
    Page<Service> findByCityIgnoreCase(String city, Pageable pageable);

    @Query("SELECT s FROM Service s JOIN s.activities a WHERE LOWER(s.city) = LOWER(:city) AND a.code = :activityCode")
    Page<Service> findByCityAndActivityCode(String city, String activityCode, Pageable pageable);

    @Query("SELECT DISTINCT s FROM Service s LEFT JOIN s.activities a WHERE " +
            "(:city IS NULL OR LOWER(s.city) = LOWER(:city)) AND " +
            "(:activityCode IS NULL OR a.code = :activityCode) AND " +
            "(:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Service> searchServices(@Param("city") String city, @Param("activityCode") String activityCode, @Param("keyword") String keyword);

    @Query("SELECT s FROM Service s WHERE " +
            "s.availability = true AND " +
            "(:city IS NULL OR LOWER(s.city) = LOWER(:city)) AND " +
            "(:activityCode IS NULL OR EXISTS (SELECT a FROM s.activities a WHERE a.code = :activityCode)) AND " +
            "NOT EXISTS (" +
            "   SELECT 1 FROM Booking b WHERE b.service.id = s.id " +
            "   AND b.bookingDate = :date " +
            "   AND b.status IN (com.hitendra.turf_booking_backend.entity.BookingStatus.CONFIRMED, com.hitendra.turf_booking_backend.entity.BookingStatus.PENDING) " +
            "   AND (:activityCode IS NULL OR b.activityCode = :activityCode) " +
            "   AND b.startTime < :endTime AND b.endTime > :startTime" +
            ") AND " +
            "NOT EXISTS (" +
            "   SELECT 1 FROM DisabledSlot ds WHERE ds.resource.service.id = s.id " +
            "   AND ds.disabledDate = :date " +
            "   AND (:activityCode IS NULL OR EXISTS (SELECT a FROM ds.resource.activities a WHERE a.code = :activityCode)) " +
            "   AND ds.startTime < :endTime AND ds.endTime > :startTime" +
            ")")
    List<Service> findAvailableServices(
            @Param("date") LocalDate date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime,
            @Param("city") String city,
            @Param("activityCode") String activityCode
    );

    @Query("SELECT new com.hitendra.turf_booking_backend.dto.service.ServiceSearchDto(" +
            "s.id, s.name, s.location, s.availability) " +
            "FROM Service s WHERE " +
            "s.availability = true AND " +
            "(:city IS NULL OR LOWER(s.city) = LOWER(:city)) AND " +
            "(:activityCode IS NULL OR EXISTS (SELECT a FROM s.activities a WHERE a.code = :activityCode)) AND " +
            "NOT EXISTS (" +
            "   SELECT 1 FROM Booking b WHERE b.service.id = s.id " +
            "   AND b.bookingDate = :date " +
            "   AND b.status IN (com.hitendra.turf_booking_backend.entity.BookingStatus.CONFIRMED, com.hitendra.turf_booking_backend.entity.BookingStatus.PENDING) " +
            "   AND (:activityCode IS NULL OR b.activityCode = :activityCode) " +
            "   AND b.startTime < :endTime AND b.endTime > :startTime" +
            ") AND " +
            "NOT EXISTS (" +
            "   SELECT 1 FROM DisabledSlot ds WHERE ds.resource.service.id = s.id " +
            "   AND ds.disabledDate = :date " +
            "   AND (:activityCode IS NULL OR EXISTS (SELECT a FROM ds.resource.activities a WHERE a.code = :activityCode)) " +
            "   AND ds.startTime < :endTime AND ds.endTime > :startTime" +
            ")")
    List<com.hitendra.turf_booking_backend.dto.service.ServiceSearchDto> findAvailableServicesDto(
            @Param("date") LocalDate date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime,
            @Param("city") String city,
            @Param("activityCode") String activityCode
    );

    // ==================== OPTIMIZED PROJECTION QUERIES ====================

    /**
     * Get lightweight service cards (projection-based).
     * Only fetches essential fields for list/card views.
     */
    @Query("""
        SELECT DISTINCT s.id as id, s.name as name, s.location as location, 
               s.city as city, s.availability as availability
        FROM Service s
        ORDER BY s.id DESC
        """)
    Page<ServiceCardProjection> findAllServicesCardProjected(Pageable pageable);

    /**
     * Get lightweight service cards by city (projection-based).
     * Uses DISTINCT to avoid duplicates from EAGER relationships.
     */
    @Query("""
        SELECT DISTINCT s.id as id, s.name as name, s.location as location, 
               s.city as city, s.availability as availability
        FROM Service s
        WHERE LOWER(s.city) = LOWER(:city)
        ORDER BY s.id DESC
        """)
    Page<ServiceCardProjection> findServiceCardsByCityProjected(@Param("city") String city, Pageable pageable);

    /**
     * Get service IDs by city (for pagination) - Step 1
     * This avoids JOIN with service_images table
     */
    @Query("""
        SELECT s.id
        FROM Service s
        WHERE LOWER(s.city) = LOWER(:city)
        ORDER BY s.id DESC
        """)
    Page<Long> findServiceIdsByCity(@Param("city") String city, Pageable pageable);

    /**
     * Get complete service entities by IDs - Step 2
     * This will fetch services with their images collection
     */
    @Query("""
        SELECT s
        FROM Service s
        WHERE s.id IN :ids
        ORDER BY s.id DESC
        """)
    List<Service> findServicesByIds(@Param("ids") List<Long> ids);

    /**
     * Get all service IDs (for pagination) - Step 1
     * This avoids JOIN with service_images table
     */
    @Query("""
        SELECT s.id
        FROM Service s
        ORDER BY s.id DESC
        """)
    Page<Long> findAllServiceIds(Pageable pageable);

    /**
     * Get only service IDs for a specific admin (for batch operations).
     */
    @Query("SELECT s.id FROM Service s WHERE s.createdBy.id = :adminProfileId")
    List<Long> findServiceIdsByCreatedById(@Param("adminProfileId") Long adminProfileId);

    /**
     * Check if service exists and belongs to admin (faster than full entity load).
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Service s WHERE s.id = :serviceId AND s.createdBy.id = :adminProfileId")
    boolean existsByIdAndCreatedById(@Param("serviceId") Long serviceId, @Param("adminProfileId") Long adminProfileId);

    /**
     * Get service name by ID (minimal data fetch).
     */
    @Query("SELECT s.name FROM Service s WHERE s.id = :serviceId")
    Optional<String> findServiceNameById(@Param("serviceId") Long serviceId);

    /**
     * Get only service IDs that are available.
     */
    @Query("SELECT s.id FROM Service s WHERE s.availability = true")
    List<Long> findAvailableServiceIds();

    /**
     * Get service IDs by city (for batch availability checking).
     */
    @Query("SELECT s.id FROM Service s WHERE LOWER(s.city) = LOWER(:city) AND s.availability = true")
    List<Long> findAvailableServiceIdsByCity(@Param("city") String city);

    /**
     * Get all distinct cities where services exist (optimized).
     */
    @Query("SELECT DISTINCT s.city FROM Service s WHERE s.city IS NOT NULL AND s.city != '' AND s.city != 'Unknown' ORDER BY s.city")
    List<String> findAllDistinctCities();

    // ==================== END OPTIMIZED PROJECTION QUERIES ====================
}
