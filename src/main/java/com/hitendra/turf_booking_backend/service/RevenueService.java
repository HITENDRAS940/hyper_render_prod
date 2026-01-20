package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.revenue.AdminRevenueReportDto;
import com.hitendra.turf_booking_backend.dto.revenue.ResourceRevenueDto;
import com.hitendra.turf_booking_backend.dto.revenue.ServiceRevenueDto;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.repository.AdminProfileRepository;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating revenue reports
 * Calculates revenue breakdowns by admin, service, and resource
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueService {

    private final BookingRepository bookingRepository;
    private final AdminProfileRepository adminProfileRepository;

    /**
     * Get complete revenue report for an admin with service-wise and resource-wise breakdown
     * Only counts CONFIRMED and COMPLETED bookings
     */
    @Transactional(readOnly = true)
    public AdminRevenueReportDto getAdminRevenueReport(Long adminId) {
        // STEP 1: Validate admin exists
        AdminProfile adminProfile = adminProfileRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin profile not found with id: " + adminId));

        log.info("Generating revenue report for admin: {} ({})", adminProfile.getUser().getName(), adminId);

        // STEP 2: Get all confirmed/completed bookings for services created by this admin
        List<Booking> adminBookings = bookingRepository.findByServiceCreatedByIdAndStatusIn(
                adminId,
                List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED)
        );

        log.info("Found {} confirmed/completed bookings for admin {}", adminBookings.size(), adminId);

        // STEP 3: Calculate overall metrics
        long totalBookings = adminBookings.size();
        double totalRevenue = adminBookings.stream()
                .mapToDouble(Booking::getAmount)
                .sum();
        double averageRevenuePerBooking = totalBookings > 0 ? totalRevenue / totalBookings : 0.0;

        // STEP 4: Group bookings by service
        Map<com.hitendra.turf_booking_backend.entity.Service, List<Booking>> bookingsByService = adminBookings.stream()
                .collect(Collectors.groupingBy(Booking::getService));

        // STEP 5: Generate service-wise revenue breakdown
        List<ServiceRevenueDto> serviceRevenues = bookingsByService.entrySet().stream()
                .map(entry -> generateServiceRevenueDto(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Double.compare(b.getTotalRevenue(), a.getTotalRevenue()))
                .collect(Collectors.toList());

        // STEP 6: Build final report
        AdminRevenueReportDto report = AdminRevenueReportDto.builder()
                .adminId(adminId)
                .adminName(adminProfile.getUser().getName())
                .adminPhone(adminProfile.getUser().getPhone())
                .totalBookings(totalBookings)
                .totalRevenue(Math.round(totalRevenue * 100.0) / 100.0)
                .averageRevenuePerBooking(Math.round(averageRevenuePerBooking * 100.0) / 100.0)
                .serviceRevenues(serviceRevenues)
                .generatedAt(LocalDateTime.now())
                .currency("INR")
                .build();

        log.info("Revenue report generated - Total Revenue: {}, Total Bookings: {}, Services: {}",
                report.getTotalRevenue(), report.getTotalBookings(), serviceRevenues.size());

        return report;
    }

    /**
     * Generate service-wise revenue breakdown with resource-wise sub-breakdown
     */
    private ServiceRevenueDto generateServiceRevenueDto(com.hitendra.turf_booking_backend.entity.Service service,
                                                        List<Booking> serviceBookings) {
        // STEP 1: Calculate service totals
        long totalBookings = serviceBookings.size();
        double totalRevenue = serviceBookings.stream()
                .mapToDouble(Booking::getAmount)
                .sum();
        double averageRevenuePerBooking = totalBookings > 0 ? totalRevenue / totalBookings : 0.0;

        // STEP 2: Group bookings by resource
        Map<ServiceResource, List<Booking>> bookingsByResource = serviceBookings.stream()
                .collect(Collectors.groupingBy(Booking::getResource));

        // STEP 3: Generate resource-wise breakdown
        List<ResourceRevenueDto> resourceRevenues = bookingsByResource.entrySet().stream()
                .map(entry -> generateResourceRevenueDto(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Double.compare(b.getTotalRevenue(), a.getTotalRevenue()))
                .collect(Collectors.toList());

        return ServiceRevenueDto.builder()
                .serviceId(service.getId())
                .serviceName(service.getName())
                .totalBookings(totalBookings)
                .totalRevenue(Math.round(totalRevenue * 100.0) / 100.0)
                .averageRevenuePerBooking(Math.round(averageRevenuePerBooking * 100.0) / 100.0)
                .resourceRevenues(resourceRevenues)
                .build();
    }

    /**
     * Generate resource-wise revenue breakdown
     */
    private ResourceRevenueDto generateResourceRevenueDto(ServiceResource resource,
                                                          List<Booking> resourceBookings) {
        long bookingCount = resourceBookings.size();
        double totalRevenue = resourceBookings.stream()
                .mapToDouble(Booking::getAmount)
                .sum();
        double averageRevenuePerBooking = bookingCount > 0 ? totalRevenue / bookingCount : 0.0;

        return ResourceRevenueDto.builder()
                .resourceId(resource.getId())
                .resourceName(resource.getName())
                .bookingCount(bookingCount)
                .totalRevenue(Math.round(totalRevenue * 100.0) / 100.0)
                .averageRevenuePerBooking(Math.round(averageRevenuePerBooking * 100.0) / 100.0)
                .build();
    }

    /**
     * Get revenue report for a specific service
     */
    @Transactional(readOnly = true)
    public ServiceRevenueDto getServiceRevenueReport(Long serviceId) {
        List<Booking> serviceBookings = bookingRepository.findByServiceIdAndStatusIn(
                serviceId,
                List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED)
        );

        com.hitendra.turf_booking_backend.entity.Service service = new com.hitendra.turf_booking_backend.entity.Service();
        service.setId(serviceId);

        return generateServiceRevenueDto(service, serviceBookings);
    }
}

