package com.hitendra.turf_booking_backend.service.accounting;

import com.hitendra.turf_booking_backend.entity.Service;
import com.hitendra.turf_booking_backend.entity.accounting.*;
import com.hitendra.turf_booking_backend.repository.accounting.CashLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * LEDGER SERVICE - THE HEART OF THE ACCOUNTING SYSTEM
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * This service is responsible for recording ALL money movement in the system.
 * Every rupee IN or OUT must pass through this service.
 *
 * CRITICAL RULES:
 * 1. Always called within a transaction
 * 2. Calculates running balance automatically
 * 3. Uses pessimistic locking to prevent race conditions
 * 4. Entries are APPEND-ONLY (never update or delete)
 *
 * USAGE:
 * - When booking is confirmed → recordCredit(BOOKING, ...)
 * - When inventory is sold → recordCredit(INVENTORY_SALE, ...)
 * - When expense is recorded → recordDebit(EXPENSE, ...)
 * - When inventory is purchased → recordDebit(INVENTORY_PURCHASE, ...)
 * - When refund is issued → recordDebit(REFUND, ...)
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final CashLedgerRepository ledgerRepository;

    /**
     * Record money IN (credit).
     *
     * @param service The service this transaction belongs to
     * @param source The source of income (BOOKING, INVENTORY_SALE)
     * @param referenceType The type of entity being referenced
     * @param referenceId The ID of the entity (booking ID, sale ID, etc.)
     * @param amount The amount received
     * @param paymentMode How payment was received
     * @param description Human-readable description
     * @param recordedBy Who recorded this transaction
     * @return The created ledger entry
     */
    @Transactional
    public CashLedger recordCredit(
        Service service,
        LedgerSource source,
        ReferenceType referenceType,
        Long referenceId,
        Double amount,
        PaymentMode paymentMode,
        String description,
        String recordedBy
    ) {
        log.info("Recording CREDIT for service {}: {} from {} (Ref: {} #{})",
            service.getId(), amount, source, referenceType, referenceId);

        // Get current balance with lock (prevents concurrent balance calculation errors)
        Double currentBalance = ledgerRepository
            .findLatestByServiceIdWithLock(service.getId())
            .map(CashLedger::getBalanceAfter)
            .orElse(0.0);

        Double newBalance = currentBalance + amount;

        CashLedger entry = CashLedger.builder()
            .service(service)
            .source(source)
            .referenceType(referenceType)
            .referenceId(referenceId)
            .creditAmount(amount)
            .debitAmount(0.0)
            .balanceAfter(newBalance)
            .paymentMode(paymentMode)
            .description(description)
            .recordedBy(recordedBy)
            .build();

        CashLedger saved = ledgerRepository.save(entry);

        log.info("CREDIT recorded: ID={}, Previous Balance={}, New Balance={}",
            saved.getId(), currentBalance, newBalance);

        return saved;
    }

    /**
     * Record money OUT (debit).
     *
     * @param service The service this transaction belongs to
     * @param source The source of expense (EXPENSE, INVENTORY_PURCHASE, REFUND)
     * @param referenceType The type of entity being referenced
     * @param referenceId The ID of the entity (expense ID, purchase ID, etc.)
     * @param amount The amount paid out
     * @param paymentMode How payment was made
     * @param description Human-readable description
     * @param recordedBy Who recorded this transaction
     * @return The created ledger entry
     */
    @Transactional
    public CashLedger recordDebit(
        Service service,
        LedgerSource source,
        ReferenceType referenceType,
        Long referenceId,
        Double amount,
        PaymentMode paymentMode,
        String description,
        String recordedBy
    ) {
        log.info("Recording DEBIT for service {}: {} from {} (Ref: {} #{})",
            service.getId(), amount, source, referenceType, referenceId);

        // Get current balance with lock
        Double currentBalance = ledgerRepository
            .findLatestByServiceIdWithLock(service.getId())
            .map(CashLedger::getBalanceAfter)
            .orElse(0.0);

        Double newBalance = currentBalance - amount;

        CashLedger entry = CashLedger.builder()
            .service(service)
            .source(source)
            .referenceType(referenceType)
            .referenceId(referenceId)
            .creditAmount(0.0)
            .debitAmount(amount)
            .balanceAfter(newBalance)
            .paymentMode(paymentMode)
            .description(description)
            .recordedBy(recordedBy)
            .build();

        CashLedger saved = ledgerRepository.save(entry);

        log.info("DEBIT recorded: ID={}, Previous Balance={}, New Balance={}",
            saved.getId(), currentBalance, newBalance);

        return saved;
    }

    /**
     * Get current balance for a service.
     */
    public Double getCurrentBalance(Long serviceId) {
        return ledgerRepository.getCurrentBalance(serviceId).orElse(0.0);
    }
}

