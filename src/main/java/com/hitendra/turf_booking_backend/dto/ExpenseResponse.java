package com.hitendra.turf_booking_backend.dto;

import com.hitendra.turf_booking_backend.entity.accounting.PaymentMode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class ExpenseResponse {
    private Long id;
    private Long serviceId;
    private String category;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private String description;
    private LocalDate expenseDate;
    private String billUrl;
    private Long createdBy;
    private Instant createdAt;
}

