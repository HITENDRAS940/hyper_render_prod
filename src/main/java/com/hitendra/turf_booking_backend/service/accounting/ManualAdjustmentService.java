package com.hitendra.turf_booking_backend.service.accounting;

import com.hitendra.turf_booking_backend.dto.accounting.ManualAdjustmentRequestDto;
import com.hitendra.turf_booking_backend.dto.accounting.ManualAdjustmentRequestDto.AdjustmentType;
import com.hitendra.turf_booking_backend.dto.accounting.ManualAdjustmentRequestDto.AdjustmentMode;
import com.hitendra.turf_booking_backend.dto.accounting.ManualAdjustmentResponseDto;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.exception.BookingException;
import com.hitendra.turf_booking_backend.repository.AdminLedgerRepository;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for handling manual cash/bank adjustments directly on admin balances.
 *
 * Use cases:
 * 1. Cash deposit to bank  – move money from cashBalance → bankBalance
 * 2. Opening / correction  – CREDIT or DEBIT on cash or bank
 * 3. Reconciliation        – fix any discrepancy on admin balances
 *
 * All adjustments are recorded in admin_ledger for a full audit trail.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ManualAdjustmentService {

    private final AdminLedgerRepository adminLedgerRepository;
    private final AuthUtil authUtil;
    private final EntityManager entityManager;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private AdminProfile loadWithLock(Long adminId) {
        AdminProfile admin = entityManager.find(AdminProfile.class, adminId, LockModeType.PESSIMISTIC_WRITE);
        if (admin == null) throw new BookingException("Admin profile not found: " + adminId);
        return admin;
    }

    private BigDecimal safe(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    // ─── Main Method ─────────────────────────────────────────────────────────

    /**
     * Record a manual CREDIT or DEBIT adjustment on the admin's cash or bank balance.
     *
     * CREDIT → adds to balance (e.g. opening balance, correction inflow)
     * DEBIT  → subtracts from balance (e.g. correction outflow) — balance must be >= amount
     *
     * paymentMode must be CASH or BANK.
     */
    @Transactional
    public ManualAdjustmentResponseDto recordManualAdjustment(ManualAdjustmentRequestDto request) {
        AdminProfile admin = loadWithLock(authUtil.getCurrentAdminProfile().getId());

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());
        AdjustmentType type = request.getType();
        AdjustmentMode mode = request.getPaymentMode();

        log.info("Manual {} adjustment for admin {}: ₹{} via {}",
                type, admin.getId(), amount, mode);

        BigDecimal previousBalance;
        BigDecimal newBalance;
        AdminLedgerType ledgerType;

        if (mode == AdjustmentMode.CASH) {
            ledgerType = AdminLedgerType.CASH;
            previousBalance = safe(admin.getCashBalance());

            if (type == AdjustmentType.DEBIT && previousBalance.compareTo(amount) < 0) {
                throw new BookingException(
                        String.format("Insufficient cash balance. Available: ₹%s, Required: ₹%s",
                                previousBalance, amount));
            }

            newBalance = type == AdjustmentType.CREDIT
                    ? previousBalance.add(amount)
                    : previousBalance.subtract(amount);
            admin.setCashBalance(newBalance);

        } else { // BANK
            ledgerType = AdminLedgerType.BANK;
            previousBalance = safe(admin.getBankBalance());

            if (type == AdjustmentType.DEBIT && previousBalance.compareTo(amount) < 0) {
                throw new BookingException(
                        String.format("Insufficient bank balance. Available: ₹%s, Required: ₹%s",
                                previousBalance, amount));
            }

            newBalance = type == AdjustmentType.CREDIT
                    ? previousBalance.add(amount)
                    : previousBalance.subtract(amount);
            admin.setBankBalance(newBalance);
        }

        // Prepare description
        String fullDescription = request.getDescription();
        if (request.getReferenceNumber() != null && !request.getReferenceNumber().trim().isEmpty()) {
            fullDescription += " [Ref: " + request.getReferenceNumber() + "]";
        }

        // Append to admin_ledger
        AdminLedgerEntryType entryType = type == AdjustmentType.CREDIT
                ? AdminLedgerEntryType.CREDIT
                : AdminLedgerEntryType.DEBIT;

        AdminLedger ledgerEntry = adminLedgerRepository.save(AdminLedger.builder()
                .admin(admin)
                .ledgerType(ledgerType)
                .entryType(entryType)
                .amount(amount)
                .balanceAfter(newBalance)
                .description(fullDescription)
                .referenceType("MANUAL_ADJUSTMENT")
                .referenceId(null)
                .build());

        // AdminProfile balance updated — managed entity, auto-flushed on commit

        log.info("Manual {} {} adjustment recorded. ledgerEntryId={}, newBalance={}",
                type, mode, ledgerEntry.getId(), newBalance);

        return ManualAdjustmentResponseDto.builder()
                .ledgerId(ledgerEntry.getId())
                .adminId(admin.getId())
                .adjustmentType(type.name())
                .paymentMode(mode.name())
                .amount(request.getAmount())
                .description(request.getDescription())
                .referenceNumber(request.getReferenceNumber())
                .previousBalance(previousBalance.doubleValue())
                .newBalance(newBalance.doubleValue())
                .recordedBy(authUtil.getCurrentUser().getPhone())
                .recordedAt(ledgerEntry.getCreatedAt())
                .build();
    }

    /**
     * Get current cash and bank balances for the current admin.
     */
    @Transactional(readOnly = true)
    public ManualAdjustmentResponseDto getCurrentBalances() {
        AdminProfile admin = authUtil.getCurrentAdminProfile();
        return ManualAdjustmentResponseDto.builder()
                .adminId(admin.getId())
                .previousBalance(safe(admin.getCashBalance()).doubleValue()) // reused as cashBalance
                .newBalance(safe(admin.getBankBalance()).doubleValue())       // reused as bankBalance
                .build();
    }
}

