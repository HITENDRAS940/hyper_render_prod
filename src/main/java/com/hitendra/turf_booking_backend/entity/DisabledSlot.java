package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;

/**
 * Represents a disabled slot for a specific service resource on a specific date.
 * Used to block slots from being booked (e.g., maintenance, private events).
 *
 * Since slots are generated dynamically, we use startTime/endTime instead of Slot reference.
 */
@Entity
@Table(name = "disabled_slots", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"resource_id", "start_time", "disabled_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisabledSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private ServiceResource resource;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "disabled_date", nullable = false)
    private LocalDate disabledDate;

    @Column
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disabled_by_admin_id")
    private AdminProfile disabledBy;

    @Column
    @Builder.Default
    private Instant createdAt = Instant.now();
}

