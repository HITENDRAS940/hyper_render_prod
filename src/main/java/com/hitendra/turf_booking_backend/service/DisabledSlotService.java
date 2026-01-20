package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.service.DisableSlotRequest;
import com.hitendra.turf_booking_backend.dto.service.DisabledSlotDto;
import com.hitendra.turf_booking_backend.entity.DisabledSlot;
import com.hitendra.turf_booking_backend.entity.ServiceResource;
import com.hitendra.turf_booking_backend.repository.DisabledSlotRepository;
import com.hitendra.turf_booking_backend.repository.ServiceResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DisabledSlotService {

    private final DisabledSlotRepository disabledSlotRepository;
    private final ServiceResourceRepository serviceResourceRepository;

    public DisabledSlotDto disableSlot(DisableSlotRequest request) {
        ServiceResource resource = serviceResourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        if (disabledSlotRepository.existsByResourceIdAndStartTimeAndDisabledDate(
                request.getResourceId(), request.getStartTime(), request.getDate())) {
            throw new RuntimeException("Slot is already disabled");
        }

        DisabledSlot disabledSlot = DisabledSlot.builder()
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .disabledDate(request.getDate())
                .reason(request.getReason())
                .build();

        DisabledSlot saved = disabledSlotRepository.save(disabledSlot);
        return convertToDto(saved);
    }

    public void enableSlot(Long disabledSlotId) {
        disabledSlotRepository.deleteById(disabledSlotId);
    }

    public List<DisabledSlotDto> getDisabledSlots(Long resourceId) {
        return disabledSlotRepository.findByResourceId(resourceId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private DisabledSlotDto convertToDto(DisabledSlot entity) {
        return DisabledSlotDto.builder()
                .id(entity.getId())
                .resourceId(entity.getResource().getId())
                .resourceName(entity.getResource().getName())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .date(entity.getDisabledDate())
                .reason(entity.getReason())
                .build();
    }
}

