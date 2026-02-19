package com.hitendra.turf_booking_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CashBankSummary {
    private BigDecimal cashCollected;
    private BigDecimal expensesCash;
    private BigDecimal cashBalance;

    private BigDecimal advanceReceived;
    private BigDecimal expensesBank;
    private BigDecimal bankBalance;
}

