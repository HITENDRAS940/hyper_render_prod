package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.service.SlotDto;
import com.hitendra.turf_booking_backend.entity.Slot;
import com.hitendra.turf_booking_backend.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SlotService {

    private final SlotRepository slotRepository;

    /**
     * Get all global slots (without turf-specific pricing)
     */
    public List<SlotDto> getAllSlots() {
        return slotRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific slot by ID
     */
    public SlotDto getSlotById(Long id) {
        Slot slot = slotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        return convertToDto(slot);
    }

    private SlotDto convertToDto(Slot slot) {
        SlotDto dto = new SlotDto();
        dto.setId(slot.getId());
        dto.setStartTime(slot.getStartTime());
        dto.setEndTime(slot.getEndTime());
        dto.setPrice(null); // Price is turf-specific, not global
        dto.setEnabled(true); // Default value when viewing global slots
        return dto;
    }
}
