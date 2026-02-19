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
    @JoinColumn(name = "venue_id", nullable = false)
    private Service venue;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private java.math.BigDecimal amount;

    @Column(name = "payment_mode")
    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode; // CASH, BANK

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

    // Legacy getter alias
    public Service getService() {
        return venue;
    }

    // Legacy setter alias
    public void setService(Service service) {
        this.venue = service;
    }
}
