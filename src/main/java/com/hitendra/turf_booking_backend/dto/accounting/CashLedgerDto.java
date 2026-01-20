package com.hitendra.turf_booking_backend.dto.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.LedgerSource;
import com.hitendra.turf_booking_backend.entity.accounting.PaymentMode;
import com.hitendra.turf_booking_backend.entity.accounting.ReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashLedgerDto {

    private Long id;
    private Long serviceId;
    private String serviceName;
    private LedgerSource source;
    private ReferenceType referenceType;
    private Long referenceId;
    private Double creditAmount;
    private Double debitAmount;
    private Double balanceAfter;
    private PaymentMode paymentMode;
    private String description;
    private String recordedBy;
    private Instant createdAt;
}

