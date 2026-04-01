package com.hitendra.turf_booking_backend.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ManagerCustomNotificationRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String body;

    /**
     * Optional target users. If omitted/empty, notification is broadcast
     * to all users who have registered push tokens.
     */
    private List<Long> userIds;

    /**
     * Optional metadata delivered in push payload.
     */
    private Map<String, Object> data;
}

