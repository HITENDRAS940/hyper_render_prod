package com.hitendra.turf_booking_backend.dto.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.ExpensePaymentMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDto {

    private Long id;
    private Long adminId;
    private String adminName;
    private Long categoryId;
    private String categoryName;
    private String categoryType;
    private String description;
    private Double amount;
    private ExpensePaymentMode paymentMode;
    private LocalDate expenseDate;
    private String referenceNumber;
    private String createdBy;
    private Instant createdAt;
}

