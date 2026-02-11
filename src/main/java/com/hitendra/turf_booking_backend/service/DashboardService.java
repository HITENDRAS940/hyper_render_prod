package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.dashboard.AdminDashboardStatsDto;
import com.hitendra.turf_booking_backend.dto.dashboard.AdminDashboardStatsDto.BookingStats;
import com.hitendra.turf_booking_backend.dto.dashboard.AdminDashboardStatsDto.RevenueStats;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Service for generating admin dashboard statistics
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardService {

    private final BookingRepository bookingRepository;

    /**
     * Get comprehensive dashboard statistics for an admin
     * Includes today's and current month's booking counts and revenue
     * broken down by online vs offline bookings
     *
     * @param adminId The admin profile ID
     * @return Dashboard statistics
     */
    public AdminDashboardStatsDto getAdminDashboardStats(Long adminId) {
        log.info("Generating dashboard statistics for admin ID: {}", adminId);

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        // Today's statistics
        Long todayOnlineBookings = bookingRepository.countOnlineBookingsByAdminAndDate(adminId, today);
        Long todayOfflineBookings = bookingRepository.countOfflineBookingsByAdminAndDate(adminId, today);

        Double todayOnlineRevenueRaw = bookingRepository.sumOnlineRevenueByAdminAndDate(adminId, today);
        Double todayOfflineRevenueRaw = bookingRepository.sumOfflineRevenueByAdminAndDate(adminId, today);

        // Monthly statistics
        Long monthlyOnlineBookings = bookingRepository.countOnlineBookingsByAdminAndDateRange(
                adminId, monthStart, monthEnd);
        Long monthlyOfflineBookings = bookingRepository.countOfflineBookingsByAdminAndDateRange(
                adminId, monthStart, monthEnd);

        Double monthlyOnlineRevenueRaw = bookingRepository.sumOnlineRevenueByAdminAndDateRange(
                adminId, monthStart, monthEnd);
        Double monthlyOfflineRevenueRaw = bookingRepository.sumOfflineRevenueByAdminAndDateRange(
                adminId, monthStart, monthEnd);

        // Convert to BigDecimal with proper rounding
        BigDecimal todayOnlineRevenue = toBigDecimal(todayOnlineRevenueRaw);
        BigDecimal todayOfflineRevenue = toBigDecimal(todayOfflineRevenueRaw);
        BigDecimal monthlyOnlineRevenue = toBigDecimal(monthlyOnlineRevenueRaw);
        BigDecimal monthlyOfflineRevenue = toBigDecimal(monthlyOfflineRevenueRaw);

        return AdminDashboardStatsDto.builder()
                .todayBooking(BookingStats.builder()
                        .online(todayOnlineBookings != null ? todayOnlineBookings : 0L)
                        .offline(todayOfflineBookings != null ? todayOfflineBookings : 0L)
                        .build())
                .monthlyBooking(BookingStats.builder()
                        .online(monthlyOnlineBookings != null ? monthlyOnlineBookings : 0L)
                        .offline(monthlyOfflineBookings != null ? monthlyOfflineBookings : 0L)
                        .build())
                .todayRevenue(RevenueStats.builder()
                        .online(todayOnlineRevenue)
                        .offline(todayOfflineRevenue)
                        .build())
                .monthlyRevenue(RevenueStats.builder()
                        .online(monthlyOnlineRevenue)
                        .offline(monthlyOfflineRevenue)
                        .build())
                .build();
    }

    /**
     * Convert Double to BigDecimal with 2 decimal places
     */
    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}

