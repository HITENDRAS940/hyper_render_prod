package com.hitendra.turf_booking_backend.service.accounting;

import com.hitendra.turf_booking_backend.dto.accounting.ManualAdjustmentRequestDto;
import com.hitendra.turf_booking_backend.dto.accounting.ManualAdjustmentResponseDto;
import com.hitendra.turf_booking_backend.entity.Service;
import com.hitendra.turf_booking_backend.entity.accounting.CashLedger;
import com.hitendra.turf_booking_backend.entity.accounting.LedgerSource;
import com.hitendra.turf_booking_backend.entity.accounting.ReferenceType;
import com.hitendra.turf_booking_backend.exception.BookingException;
import com.hitendra.turf_booking_backend.repository.ServiceRepository;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling manual cash/bank adjustments by admins.
 *
 * Use cases:
 * 1. Cash deposit to bank - Record money movement from cash to bank
 * 2. Manual corrections - Fix errors or missing entries
 * 3. Initial balance setup - Set starting balance for a service
 * 4. Reconciliation - Adjust for discrepancies
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ManualAdjustmentService {

    private final LedgerService ledgerService;
    private final ServiceRepository serviceRepository;
    private final AuthUtil authUtil;

    /**
     * Record a manual adjustment (credit or debit) in the ledger.
     *
     * @param serviceId The service ID to adjust
     * @param request The adjustment details
     * @return Response with ledger entry details
     */
    @Transactional
    public ManualAdjustmentResponseDto recordManualAdjustment(Long serviceId, ManualAdjustmentRequestDto request) {
        log.info("Recording manual {} adjustment for service {}: {} via {}",
                request.getType(), serviceId, request.getAmount(), request.getPaymentMode());

        // Get current admin
        Long currentAdminId = authUtil.getCurrentAdminProfile().getId();

        // Validate service exists and belongs to current admin
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new BookingException("Service not found: " + serviceId));

        if (!service.getCreatedBy().getId().equals(currentAdminId)) {
            throw new BookingException("Access denied: This service belongs to another admin");
        }

        // Get previous balance
        Double previousBalance = ledgerService.getCurrentBalance(serviceId);

        // Prepare description with reference number if provided
        String fullDescription = request.getDescription();
        if (request.getReferenceNumber() != null && !request.getReferenceNumber().trim().isEmpty()) {
            fullDescription += " [Ref: " + request.getReferenceNumber() + "]";
        }

        // Get recorded by
        String recordedBy = authUtil.getCurrentUser().getPhone();

        // Use a unique reference ID for adjustments (negative to avoid conflicts)
        Long referenceId = System.currentTimeMillis();

        CashLedger ledgerEntry;

        // Record credit or debit based on type
        if (request.getType() == ManualAdjustmentRequestDto.AdjustmentType.CREDIT) {
            ledgerEntry = ledgerService.recordCredit(
                    service,
                    LedgerSource.ADJUSTMENT,
                    ReferenceType.ADJUSTMENT,
                    referenceId,
                    request.getAmount(),
                    request.getPaymentMode(),
                    fullDescription,
                    recordedBy
            );
            log.info("Manual CREDIT adjustment recorded: ID={}, Amount={}, New Balance={}",
                    ledgerEntry.getId(), request.getAmount(), ledgerEntry.getBalanceAfter());
        } else {
            ledgerEntry = ledgerService.recordDebit(
                    service,
                    LedgerSource.ADJUSTMENT,
                    ReferenceType.ADJUSTMENT,
                    referenceId,
                    request.getAmount(),
                    request.getPaymentMode(),
                    fullDescription,
                    recordedBy
            );
            log.info("Manual DEBIT adjustment recorded: ID={}, Amount={}, New Balance={}",
                    ledgerEntry.getId(), request.getAmount(), ledgerEntry.getBalanceAfter());
        }

        // Build response
        return ManualAdjustmentResponseDto.builder()
                .ledgerId(ledgerEntry.getId())
                .serviceId(service.getId())
                .serviceName(service.getName())
                .adjustmentType(request.getType().name())
                .amount(request.getAmount())
                .paymentMode(request.getPaymentMode())
                .description(request.getDescription())
                .referenceNumber(request.getReferenceNumber())
                .previousBalance(previousBalance)
                .newBalance(ledgerEntry.getBalanceAfter())
                .recordedBy(recordedBy)
                .recordedAt(ledgerEntry.getCreatedAt())
                .build();
    }

    /**
     * Get current balance for a service.
     *
     * @param serviceId The service ID
     * @return Current balance
     */
    public Double getCurrentBalance(Long serviceId) {
        // Validate service exists and belongs to current admin
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new BookingException("Service not found: " + serviceId));

        Long currentAdminId = authUtil.getCurrentAdminProfile().getId();
        if (!service.getCreatedBy().getId().equals(currentAdminId)) {
            throw new BookingException("Access denied: This service belongs to another admin");
        }

        return ledgerService.getCurrentBalance(serviceId);
    }
}

