package com.hitendra.turf_booking_backend.entity.accounting;

import com.hitendra.turf_booking_backend.entity.AdminProfile;
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
    @Index(name = "idx_admin_expense_date", columnList = "admin_profile_id, expense_date"),
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
    @JoinColumn(name = "admin_profile_id", nullable = false)
    private AdminProfile adminProfile;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private java.math.BigDecimal amount;

    @Column(name = "payment_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private ExpensePaymentMode paymentMode; // CASH or BANK only

    @Column(nullable = false)
    private LocalDate expenseDate;

    @Column(name = "bill_url")
    private String billUrl;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
