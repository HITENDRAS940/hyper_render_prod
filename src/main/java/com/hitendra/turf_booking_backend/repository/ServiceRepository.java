package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    @Modifying
    @Transactional
    void deleteServiceById(Long id);

    List<Service> findByCreatedById(Long adminProfileId);
    Page<Service> findByCreatedById(Long adminProfileId, Pageable pageable);
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
}
