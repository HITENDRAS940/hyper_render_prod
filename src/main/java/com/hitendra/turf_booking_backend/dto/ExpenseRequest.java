package com.hitendra.turf_booking_backend.dto;

import com.hitendra.turf_booking_backend.entity.accounting.PaymentMode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ExpenseRequest {
    private String category;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private String description;
    private LocalDate expenseDate;
    private String billUrl;
}

