package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.activity.GetActivityDto;
import com.hitendra.turf_booking_backend.entity.Activity;
import com.hitendra.turf_booking_backend.repository.ActivityRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;

    public List<GetActivityDto> getAllActivities(){
        List<Activity> activities = activityRepository.findAll();

        return activities.stream().map(this::convertToDto).collect(Collectors.toList());

    }

    private GetActivityDto convertToDto(Activity activity){
        GetActivityDto dto = new GetActivityDto();
        dto.setId(activity.getId());
        dto.setName(activity.getName());
        return dto;
    }

    public Activity getActivityById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + id));
    }
}
