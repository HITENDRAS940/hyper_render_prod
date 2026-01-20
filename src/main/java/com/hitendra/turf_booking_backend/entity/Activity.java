package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // FOOTBALL, CRICKET, BOWLING, PADEL

    @Column(nullable = false)
    private String name; // Football, Cricket, Bowling, Padel Ball

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
}

