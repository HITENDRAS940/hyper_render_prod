package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.service.DisabledSlotDto;
import com.hitendra.turf_booking_backend.dto.service.DisableSlotRequest;
import com.hitendra.turf_booking_backend.dto.service.SlotAvailabilityDto;
import com.hitendra.turf_booking_backend.dto.service.SlotStatusResponseDto;
import com.hitendra.turf_booking_backend.dto.service.ServiceSlotAvailabilityDto;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * @deprecated This service is obsolete. Slots are now dynamically generated.
 * Use ResourceSlotService and DisabledSlotService instead.
 */
@Deprecated
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceSlotService {

    // Dependencies removed to avoid errors - this service should not be used

    /**
     * @deprecated Slots are now dynamically generated. Use ResourceSlotService.getSlotAvailability instead.
     */
    @Deprecated
    public List<ServiceSlotAvailabilityDto> getSlotAvailabilityForDate(Long serviceId, LocalDate date) {
        throw new UnsupportedOperationException("This method is deprecated. Use ResourceSlotService instead.");
    }

    /**
     * @deprecated Slots are now dynamically generated. Use ResourceSlotService.getSlotAvailability instead.
     */
    @Deprecated
    public List<SlotAvailabilityDto> getSimpleSlotAvailability(Long serviceId, LocalDate date) {
        throw new UnsupportedOperationException("This method is deprecated. Use ResourceSlotService instead.");
    }

    /**
     * @deprecated Slots are now dynamically generated. Use ResourceSlotService.getDetailedSlotsByResourceAndDate instead.
     */
    @Deprecated
    public SlotStatusResponseDto getSlotStatusForDate(Long serviceId, LocalDate date) {
        throw new UnsupportedOperationException("This method is deprecated. Use ResourceSlotService instead.");
    }

    /**
     * @deprecated Slots are now dynamically generated. This method is no longer supported.
     */
    @Deprecated
    public List<ServiceSlotAvailabilityDto> getAllBookedDatesForService(Long serviceId) {
        throw new UnsupportedOperationException("This method is deprecated. Slots are dynamically generated.");
    }

    /**
     * @deprecated Slots are now dynamically generated. This method is no longer supported.
     */
    @Deprecated
    public List<LocalDate> getBookedDatesForSlot(Long serviceId, Long slotId) {
        throw new UnsupportedOperationException("This method is deprecated. Slots are dynamically generated.");
    }

    /**
     * @deprecated Slots are now dynamically generated. This method is no longer supported.
     */
    @Deprecated
    @Transactional
    public boolean bookSlotForDate(Long serviceId, Long slotId, LocalDate date) {
        throw new UnsupportedOperationException("This method is deprecated. Use SlotBookingService instead.");
    }

    /**
     * @deprecated Slots are now dynamically generated. This method is no longer supported.
     */
    @Deprecated
    @Transactional
    public boolean cancelSlotBooking(Long serviceId, Long slotId, LocalDate date) {
        throw new UnsupportedOperationException("This method is deprecated. Use BookingService.cancelBooking instead.");
    }

    /**
     * @deprecated Slots are now dynamically generated. This method is no longer supported.
     */
    @Deprecated
    public boolean isSlotAvailable(Long serviceId, Long slotId, LocalDate date) {
        throw new UnsupportedOperationException("This method is deprecated. Use ResourceSlotService instead.");
    }

    /**
     * @deprecated Disabled slots now use DisabledSlotService
     */
    @Deprecated
    public boolean isSlotDisabledForDate(Long serviceId, Long slotId, LocalDate date) {
        throw new UnsupportedOperationException("This method is deprecated. Use DisabledSlotService instead.");
    }

    /**
     * @deprecated Disabled slots now use DisabledSlotService
     */
    @Deprecated
    @Transactional
    public DisabledSlotDto disableSlotForDate(DisableSlotRequest request) {
        throw new UnsupportedOperationException("This method is deprecated. Use DisabledSlotService instead.");
    }

    /**
     * @deprecated Disabled slots now use DisabledSlotService
     */
    @Deprecated
    @Transactional
    public void enableSlotForDate(Long serviceId, Long slotId, LocalDate date) {
        throw new UnsupportedOperationException("This method is deprecated. Use DisabledSlotService instead.");
    }

    /**
     * @deprecated Disabled slots now use DisabledSlotService
     */
    @Deprecated
    public List<DisabledSlotDto> getDisabledSlotsForDate(Long serviceId, LocalDate date) {
        throw new UnsupportedOperationException("This method is deprecated. Use DisabledSlotService instead.");
    }

    /**
     * @deprecated Disabled slots now use DisabledSlotService
     */
    @Deprecated
    public List<DisabledSlotDto> getAllDisabledSlotsForService(Long serviceId) {
        throw new UnsupportedOperationException("This method is deprecated. Use DisabledSlotService instead.");
    }

    /**
     * @deprecated Disabled slots now use DisabledSlotService
     */
    @Deprecated
    public List<DisabledSlotDto> getDisabledSlotsForDateRange(Long serviceId, LocalDate startDate, LocalDate endDate) {
        throw new UnsupportedOperationException("This method is deprecated. Use DisabledSlotService instead.");
    }
}
