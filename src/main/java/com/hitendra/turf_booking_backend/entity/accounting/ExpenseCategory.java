package com.hitendra.turf_booking_backend.entity.accounting;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Master table for expense categories.
 * Examples: Electricity, Maintenance, Inventory Purchase, Staff Salary
 */
@Entity
@Table(name = "expense_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseType type;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

