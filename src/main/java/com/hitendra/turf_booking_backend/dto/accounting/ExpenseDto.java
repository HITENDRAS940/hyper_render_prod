package com.hitendra.turf_booking_backend.dto.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.PaymentMode;
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
    private Long serviceId;
    private String serviceName;
    private Long categoryId;
    private String categoryName;
    private String categoryType;
    private String description;
    private Double amount;
    private PaymentMode paymentMode;
    private LocalDate expenseDate;
    private String referenceNumber;
    private String createdBy;
    private Instant createdAt;
}

