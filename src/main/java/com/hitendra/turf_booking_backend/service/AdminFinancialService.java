package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.financial.AdminDueSummaryDto;
import com.hitendra.turf_booking_backend.dto.financial.AdminFinancialOverviewDto;
import com.hitendra.turf_booking_backend.dto.financial.SettlementDto;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.exception.BookingException;
import com.hitendra.turf_booking_backend.repository.AdminProfileRepository;
import com.hitendra.turf_booking_backend.repository.FinancialTransactionRepository;
import com.hitendra.turf_booking_backend.repository.SettlementRepository;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ADMIN FINANCIAL SERVICE
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Central service for all admin-level financial tracking.
 *
 * Money Flow:
 *  1. ADVANCE_ONLINE  → platform collects, goes to pendingOnlineAmount
 *  2. VENUE_CASH      → admin collects cash, goes to cashBalance
 *  3. VENUE_BANK      → admin collects online/UPI direct, goes to bankBalance
 *  4. SETTLEMENT      → manager transfers pendingOnlineAmount → admin bankBalance
 *
 * currentBalance = cashBalance + bankBalance  (computed, never stored)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFinancialService {

    private final AdminProfileRepository adminProfileRepository;
    private final SettlementRepository settlementRepository;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final AuthUtil authUtil;
    private final EntityManager entityManager;

    // ─── Private Helpers ──────────────────────────────────────────────────────

    /**
     * Load AdminProfile with a PESSIMISTIC_WRITE lock to prevent concurrent balance corruption.
     */
    private AdminProfile loadWithLock(Long adminId) {
        AdminProfile admin = entityManager.find(AdminProfile.class, adminId, LockModeType.PESSIMISTIC_WRITE);
        if (admin == null) {
            throw new BookingException("Admin profile not found: " + adminId);
        }
        return admin;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ─── Flow 1: Advance Online (Platform Collected) ──────────────────────────

    /**
     * Called when Razorpay captures the online advance payment.
     *
     * Effect:
     *  admin.pendingOnlineAmount          += amount
     *  admin.totalPlatformOnlineCollected += amount
     */
    @Transactional
    public void recordAdvanceOnlinePayment(Long adminId, BigDecimal amount, Long bookingId) {
        log.info("Recording ADVANCE_ONLINE payment: adminId={}, amount={}, bookingId={}",
                adminId, amount, bookingId);

        AdminProfile admin = loadWithLock(adminId);

        admin.setPendingOnlineAmount(safe(admin.getPendingOnlineAmount()).add(amount));
        admin.setTotalPlatformOnlineCollected(safe(admin.getTotalPlatformOnlineCollected()).add(amount));

        adminProfileRepository.save(admin);

        financialTransactionRepository.save(FinancialTransaction.builder()
                .admin(admin)
                .type(FinancialTransactionType.ADVANCE_ONLINE)
                .amount(amount)
                .referenceId(bookingId)
                .build());

        log.info("ADVANCE_ONLINE recorded. pendingOnlineAmount={}",
                admin.getPendingOnlineAmount());
    }

    // ─── Flow 2: Remaining Payment at Venue – CASH ────────────────────────────

    /**
     * Called when remaining payment is collected at venue in CASH.
     *
     * Effect:
     *  admin.cashBalance       += amount
     *  admin.totalCashCollected += amount
     */
    @Transactional
    public void recordVenueCashPayment(Long adminId, BigDecimal amount, Long bookingId) {
        log.info("Recording VENUE_CASH payment: adminId={}, amount={}, bookingId={}",
                adminId, amount, bookingId);

        AdminProfile admin = loadWithLock(adminId);

        admin.setCashBalance(safe(admin.getCashBalance()).add(amount));
        admin.setTotalCashCollected(safe(admin.getTotalCashCollected()).add(amount));

        adminProfileRepository.save(admin);

        financialTransactionRepository.save(FinancialTransaction.builder()
                .admin(admin)
                .type(FinancialTransactionType.VENUE_CASH)
                .amount(amount)
                .referenceId(bookingId)
                .build());

        log.info("VENUE_CASH recorded. cashBalance={}", admin.getCashBalance());
    }

    // ─── Flow 3: Remaining Payment at Venue – Online Direct to Admin ──────────

    /**
     * Called when remaining payment is paid via online/UPI directly to admin's bank.
     * This is NOT platform money — goes straight to bankBalance.
     *
     * Effect:
     *  admin.bankBalance        += amount
     *  admin.totalBankCollected += amount
     */
    @Transactional
    public void recordVenueBankPayment(Long adminId, BigDecimal amount, Long bookingId) {
        log.info("Recording VENUE_BANK payment: adminId={}, amount={}, bookingId={}",
                adminId, amount, bookingId);

        AdminProfile admin = loadWithLock(adminId);

        admin.setBankBalance(safe(admin.getBankBalance()).add(amount));
        admin.setTotalBankCollected(safe(admin.getTotalBankCollected()).add(amount));

        adminProfileRepository.save(admin);

        financialTransactionRepository.save(FinancialTransaction.builder()
                .admin(admin)
                .type(FinancialTransactionType.VENUE_BANK)
                .amount(amount)
                .referenceId(bookingId)
                .build());

        log.info("VENUE_BANK recorded. bankBalance={}", admin.getBankBalance());
    }

    // ─── Settlement: Manager → Admin Bank ─────────────────────────────────────

    /**
     * Manager settles the pending online amount to the admin's bank.
     *
     * Rules:
     *  - Cannot settle more than pendingOnlineAmount
     *  - Fully transactional
     *
     * Effect:
     *  admin.pendingOnlineAmount -= amount
     *  admin.bankBalance         += amount
     *  admin.totalSettledAmount  += amount
     */
    @Transactional
    public SettlementDto settleAmount(Long adminId, BigDecimal amount,
                                      SettlementPaymentMode paymentMode, String reference) {
        log.info("Settling amount for adminId={}: amount={}, mode={}", adminId, amount, paymentMode);

        AdminProfile admin = loadWithLock(adminId);

        BigDecimal pending = safe(admin.getPendingOnlineAmount());
        if (amount.compareTo(pending) > 0) {
            throw new BookingException(
                String.format("Cannot settle ₹%.2f — only ₹%.2f is pending for admin %d",
                        amount, pending, adminId));
        }

        // Resolve the manager performing this settlement
        Long managerId = null;
        try {
            managerId = authUtil.getCurrentUser().getId();
        } catch (Exception e) {
            log.warn("Could not resolve current manager ID during settlement: {}", e.getMessage());
        }

        // Update balances
        admin.setPendingOnlineAmount(pending.subtract(amount));
        admin.setBankBalance(safe(admin.getBankBalance()).add(amount));
        admin.setTotalSettledAmount(safe(admin.getTotalSettledAmount()).add(amount));

        adminProfileRepository.save(admin);

        // Persist settlement record
        Settlement settlement = settlementRepository.save(Settlement.builder()
                .admin(admin)
                .amount(amount)
                .paymentMode(paymentMode)
                .status(SettlementStatus.SUCCESS)
                .settledByManagerId(managerId)
                .settlementReference(reference)
                .build());

        // Audit log
        financialTransactionRepository.save(FinancialTransaction.builder()
                .admin(admin)
                .type(FinancialTransactionType.SETTLEMENT)
                .amount(amount)
                .referenceId(settlement.getId())
                .build());

        log.info("Settlement complete. Settlement ID={}, pendingOnlineAmount={}, bankBalance={}",
                settlement.getId(), admin.getPendingOnlineAmount(), admin.getBankBalance());

        return mapToSettlementDto(settlement, admin);
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    /**
     * Full financial overview for a single admin.
     * Used by both Manager (GET /manager/admin/{id}/finance) and
     * Admin (GET /admin/ledger-summary).
     */
    @Transactional(readOnly = true)
    public AdminFinancialOverviewDto getAdminFinancialOverview(Long adminId) {
        AdminProfile admin = adminProfileRepository.findById(adminId)
                .orElseThrow(() -> new BookingException("Admin not found: " + adminId));
        return mapToOverviewDto(admin);
    }

    /**
     * Due summary for all admins — used by Manager dashboard.
     * GET /manager/admins/due-summary
     */
    @Transactional(readOnly = true)
    public List<AdminDueSummaryDto> getAllAdminDueSummary() {
        return adminProfileRepository.findAll().stream()
                .map(admin -> AdminDueSummaryDto.builder()
                        .adminId(admin.getId())
                        .adminName(admin.getUser() != null ? admin.getUser().getName() : "Unknown")
                        .pendingOnlineAmount(safe(admin.getPendingOnlineAmount()))
                        .totalSettled(safe(admin.getTotalSettledAmount()))
                        .build())
                .collect(Collectors.toList());
    }

    // ─── Mapping Helpers ──────────────────────────────────────────────────────

    private AdminFinancialOverviewDto mapToOverviewDto(AdminProfile admin) {
        BigDecimal cash = safe(admin.getCashBalance());
        BigDecimal bank = safe(admin.getBankBalance());
        return AdminFinancialOverviewDto.builder()
                .adminId(admin.getId())
                .adminName(admin.getUser() != null ? admin.getUser().getName() : "Unknown")
                .cashBalance(cash)
                .bankBalance(bank)
                .currentBalance(cash.add(bank))
                .pendingOnlineAmount(safe(admin.getPendingOnlineAmount()))
                .totalCashCollected(safe(admin.getTotalCashCollected()))
                .totalBankCollected(safe(admin.getTotalBankCollected()))
                .totalPlatformOnlineCollected(safe(admin.getTotalPlatformOnlineCollected()))
                .totalSettledAmount(safe(admin.getTotalSettledAmount()))
                .build();
    }

    private SettlementDto mapToSettlementDto(Settlement s, AdminProfile admin) {
        return SettlementDto.builder()
                .id(s.getId())
                .adminId(admin.getId())
                .adminName(admin.getUser() != null ? admin.getUser().getName() : "Unknown")
                .amount(s.getAmount())
                .paymentMode(s.getPaymentMode())
                .status(s.getStatus())
                .settledByManagerId(s.getSettledByManagerId())
                .settlementReference(s.getSettlementReference())
                .createdAt(s.getCreatedAt())
                .build();
    }
}

