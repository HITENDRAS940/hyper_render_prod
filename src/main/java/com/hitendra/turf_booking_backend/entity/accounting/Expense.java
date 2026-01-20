package com.hitendra.turf_booking_backend.entity.accounting;

import com.hitendra.turf_booking_backend.entity.Service;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Records all outgoing money (expenses).
 * Examples: Electricity bills, maintenance, salaries, inventory purchases.
 *
 * IMPORTANT: This table is APPEND-ONLY. Never delete records.
 * Use adjustment entries for corrections.
 */
@Entity
@Table(name = "expenses", indexes = {
    @Index(name = "idx_service_expense_date", columnList = "service_id, expense_date"),
    @Index(name = "idx_category", columnList = "category_id"),
    @Index(name = "idx_expense_date", columnList = "expense_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode paymentMode;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @Column
    private String referenceNumber; // Bill number, transaction ID, etc.

    @Column
    private String createdBy; // Admin/Staff who recorded this

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

