package com.hitendra.turf_booking_backend.entity.accounting;

import com.hitendra.turf_booking_backend.entity.AdminProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Admin-specific expense categories.
 * Each admin has their own set of categories.
 * Examples: Electricity, Maintenance, Inventory Purchase, Staff Salary
 *
 * ADMIN-SPECIFIC:
 * - Each admin can create their own categories
 * - Category names can be duplicated across admins
 * - Unique constraint: (admin_profile_id, name)
 */
@Entity
@Table(name = "expense_categories",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_expense_category_admin_name",
                         columnNames = {"admin_profile_id", "name"})
    },
    indexes = {
        @Index(name = "idx_expense_category_admin", columnList = "admin_profile_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_profile_id", nullable = false)
    private AdminProfile adminProfile;

    @Column(nullable = false)
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

