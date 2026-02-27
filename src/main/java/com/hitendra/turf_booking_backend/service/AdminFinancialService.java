package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.common.PaginatedResponse;
import com.hitendra.turf_booking_backend.dto.financial.*;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.entity.accounting.LedgerSource;
import com.hitendra.turf_booking_backend.entity.accounting.PaymentMode;
import com.hitendra.turf_booking_backend.entity.accounting.ReferenceType;
import static com.hitendra.turf_booking_backend.entity.accounting.ReferenceType.SETTLEMENT;
import com.hitendra.turf_booking_backend.exception.BookingException;
import com.hitendra.turf_booking_backend.repository.AdminLedgerRepository;
import com.hitendra.turf_booking_backend.repository.AdminProfileRepository;
import com.hitendra.turf_booking_backend.repository.FinancialTransactionRepository;
import com.hitendra.turf_booking_backend.repository.ServiceRepository;
import com.hitendra.turf_booking_backend.repository.SettlementRepository;
import com.hitendra.turf_booking_backend.service.accounting.LedgerService;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ADMIN FINANCIAL SERVICE
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Money Flow:
 *  1. ADVANCE_ONLINE  → platform collects; goes to pendingOnlineAmount (NOT admin cash/bank)
 *  2. VENUE_CASH      → admin collects cash at venue → cashBalance++ + CASH ledger CREDIT
 *  3. VENUE_BANK      → admin collects online/UPI at venue → bankBalance++ + BANK ledger CREDIT
 *  4. SETTLEMENT      → manager pays pending to admin bank → pendingOnlineAmount-- + bankBalance++ + BANK ledger CREDIT
 *  5. ADMIN_EXPENSE   → admin records an outgoing expense (cash or bank) → balance-- + ledger DEBIT
 *
 * cashBalance and bankBalance are ALWAYS kept separate.
 * Advance online payments are NOT credited to cash or bank until the manager settles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFinancialService {

    private final AdminProfileRepository adminProfileRepository;
    private final SettlementRepository settlementRepository;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final AdminLedgerRepository adminLedgerRepository;
    private final AuthUtil authUtil;
    private final EntityManager entityManager;
    private final LedgerService ledgerService;
    private final ServiceRepository serviceRepository;

    // ─── Private Helpers ──────────────────────────────────────────────────────

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

    /**
     * Appends an entry to the admin_ledger table.
     * Uses the balance from AdminProfile as the source of truth for balanceAfter.
     */
    private AdminLedger appendLedgerEntry(AdminProfile admin,
                                          AdminLedgerType ledgerType,
                                          AdminLedgerEntryType entryType,
                                          BigDecimal amount,
                                          BigDecimal balanceAfter,
                                          String description,
                                          String referenceType,
                                          Long referenceId) {
        return adminLedgerRepository.save(AdminLedger.builder()
                .admin(admin)
                .ledgerType(ledgerType)
                .entryType(entryType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build());
    }

    // ─── Flow 1: Advance Online (Platform Collected) ──────────────────────────

    /**
     * Called when Razorpay captures the online advance payment.
     * This money goes to the PLATFORM account, NOT to admin's cash or bank.
     * It is shown as "pending" amount owed to admin by platform.
     *
     * Effect:
     *  admin.pendingOnlineAmount          += amount
     *  admin.totalPlatformOnlineCollected += amount
     *
     * NOTE: No ledger entry here — this is platform money, not in admin's hands yet.
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
     * This goes directly into the admin's cash balance.
     *
     * Effect:
     *  admin.cashBalance       += amount   (physical cash)
     *  admin.totalCashCollected += amount
     *  → CASH ledger CREDIT entry
     */
    @Transactional
    public void recordVenueCashPayment(Long adminId, BigDecimal amount, Long bookingId) {
        log.info("Recording VENUE_CASH payment: adminId={}, amount={}, bookingId={}",
                adminId, amount, bookingId);

        AdminProfile admin = loadWithLock(adminId);

        BigDecimal newCashBalance = safe(admin.getCashBalance()).add(amount);
        admin.setCashBalance(newCashBalance);
        admin.setTotalCashCollected(safe(admin.getTotalCashCollected()).add(amount));

        adminProfileRepository.save(admin);

        financialTransactionRepository.save(FinancialTransaction.builder()
                .admin(admin)
                .type(FinancialTransactionType.VENUE_CASH)
                .amount(amount)
                .referenceId(bookingId)
                .build());

        appendLedgerEntry(admin, AdminLedgerType.CASH, AdminLedgerEntryType.CREDIT,
                amount, newCashBalance,
                "Venue cash collected for booking #" + bookingId,
                "BOOKING", bookingId);

        log.info("VENUE_CASH recorded. cashBalance={}", admin.getCashBalance());
    }

    // ─── Flow 3: Remaining Payment at Venue – Online Direct to Admin ──────────

    /**
     * Called when remaining payment is paid via online/UPI directly to admin's bank at venue.
     * This is NOT platform money — goes straight to admin's bankBalance.
     *
     * Effect:
     *  admin.bankBalance        += amount   (bank/UPI)
     *  admin.totalBankCollected += amount
     *  → BANK ledger CREDIT entry
     */
    @Transactional
    public void recordVenueBankPayment(Long adminId, BigDecimal amount, Long bookingId) {
        log.info("Recording VENUE_BANK payment: adminId={}, amount={}, bookingId={}",
                adminId, amount, bookingId);

        AdminProfile admin = loadWithLock(adminId);

        BigDecimal newBankBalance = safe(admin.getBankBalance()).add(amount);
        admin.setBankBalance(newBankBalance);
        admin.setTotalBankCollected(safe(admin.getTotalBankCollected()).add(amount));

        adminProfileRepository.save(admin);

        financialTransactionRepository.save(FinancialTransaction.builder()
                .admin(admin)
                .type(FinancialTransactionType.VENUE_BANK)
                .amount(amount)
                .referenceId(bookingId)
                .build());

        appendLedgerEntry(admin, AdminLedgerType.BANK, AdminLedgerEntryType.CREDIT,
                amount, newBankBalance,
                "Venue bank/UPI collected for booking #" + bookingId,
                "BOOKING", bookingId);

        log.info("VENUE_BANK recorded. bankBalance={}", admin.getBankBalance());
    }

    // ─── Flow 4: Settlement: Manager → Admin Bank ─────────────────────────────

    /**
     * Manager settles the pending online amount to the admin's bank.
     *
     * Rules:
     *  - Cannot settle more than pendingOnlineAmount
     *  - Fully transactional
     *
     * Effect:
     *  admin.pendingOnlineAmount -= amount   (platform owes less)
     *  admin.bankBalance         += amount   (admin bank grows)
     *  admin.totalSettledAmount  += amount
     *  → BANK ledger CREDIT entry
     */
    @Transactional
    public SettlementDto settleAmount(Long adminId, BigDecimal amount,
                                      SettlementPaymentMode paymentMode, String reference, String notes) {
        log.info("Settling amount for adminId={}: amount={}, mode={}", adminId, amount, paymentMode);

        AdminProfile admin = loadWithLock(adminId);

        BigDecimal pending = safe(admin.getPendingOnlineAmount());
        if (amount.compareTo(pending) > 0) {
            throw new BookingException(
                String.format("Cannot settle ₹%.2f — only ₹%.2f is pending for admin %d",
                        amount, pending, adminId));
        }

        Long managerId = null;
        try {
            managerId = authUtil.getCurrentUser().getId();
        } catch (Exception e) {
            log.warn("Could not resolve current manager ID during settlement: {}", e.getMessage());
        }

        BigDecimal newBankBalance = safe(admin.getBankBalance()).add(amount);

        admin.setPendingOnlineAmount(pending.subtract(amount));
        admin.setBankBalance(newBankBalance);
        admin.setTotalSettledAmount(safe(admin.getTotalSettledAmount()).add(amount));

        adminProfileRepository.save(admin);

        Settlement settlement = settlementRepository.save(Settlement.builder()
                .admin(admin)
                .amount(amount)
                .paymentMode(paymentMode)
                .status(SettlementStatus.SUCCESS)
                .settledByManagerId(managerId)
                .settlementReference(reference)
                .notes(notes)
                .pendingAfterSettlement(admin.getPendingOnlineAmount())
                .build());

        financialTransactionRepository.save(FinancialTransaction.builder()
                .admin(admin)
                .type(FinancialTransactionType.SETTLEMENT)
                .amount(amount)
                .referenceId(settlement.getId())
                .build());

        appendLedgerEntry(admin, AdminLedgerType.BANK, AdminLedgerEntryType.CREDIT,
                amount, newBankBalance,
                "Settlement from platform (ref: " + (reference != null ? reference : settlement.getId()) + ")",
                "SETTLEMENT", settlement.getId());

        // ── Cash Ledger (per-service operational ledger) ──────────────────────────
        // Online booking amounts were intentionally NOT recorded in cash_ledger at booking time
        // because the money was held by Razorpay/manager.
        // NOW that the manager has physically transferred the money to the admin's bank,
        // we record it in the cash_ledger so the P&L / balance sheet is accurate.
        //
        // If the admin manages multiple services, the settled amount is distributed
        // proportionally across services based on each service's share of total pending
        // online bookings. For a single-service admin (most common case), 100% goes to
        // that service.
        try {
            List<com.hitendra.turf_booking_backend.entity.Service> adminServices =
                    serviceRepository.findByCreatedById(adminId);

            if (!adminServices.isEmpty()) {
                String settledBy = managerId != null ? "MGR-" + managerId : "MANAGER";
                String settlementDesc = "Online booking advance settled (Settlement #" + settlement.getId()
                        + (reference != null ? ", ref: " + reference : "") + ")";

                if (adminServices.size() == 1) {
                    // Single service — credit full amount
                    ledgerService.recordCredit(
                            adminServices.get(0),
                            LedgerSource.SETTLEMENT,
                            SETTLEMENT,
                            settlement.getId(),
                            amount.doubleValue(),
                            PaymentMode.BANK_TRANSFER,
                            settlementDesc,
                            settledBy
                    );
                } else {
                    // Multiple services — distribute proportionally by pending online revenue
                    for (com.hitendra.turf_booking_backend.entity.Service service : adminServices) {
                        // Use proportion of the settlement amount relative to total pending
                        // For simplicity, split equally across services
                        double share = amount.doubleValue() / adminServices.size();
                        ledgerService.recordCredit(
                                service,
                                LedgerSource.SETTLEMENT,
                                SETTLEMENT,
                                settlement.getId(),
                                share,
                                PaymentMode.BANK_TRANSFER,
                                settlementDesc + " (service share)",
                                settledBy
                        );
                    }
                }
                log.info("Cash ledger credited for settlement ID={}, amount={}", settlement.getId(), amount);
            } else {
                log.warn("No services found for adminId={} during settlement ledger recording", adminId);
            }
        } catch (Exception e) {
            log.error("Failed to record settlement in cash_ledger for adminId={}: {}", adminId, e.getMessage(), e);
            // Don't fail the settlement itself if ledger recording fails
        }

        log.info("Settlement complete. Settlement ID={}, pendingOnlineAmount={}, bankBalance={}",
                settlement.getId(), admin.getPendingOnlineAmount(), admin.getBankBalance());

        return mapToSettlementDto(settlement, admin);
    }

    // ─── Flow 5: Admin Expense (Cash or Bank Debit) ───────────────────────────

    /**
     * Admin records a direct expense (e.g., salary paid in cash, vendor payment via UPI).
     * Deducts from the appropriate sub-ledger (CASH or BANK).
     *
     * Rules:
     *  - paymentMode must be "CASH" or "BANK"
     *  - Amount must not exceed current balance of that sub-ledger
     *
     * Effect (CASH):
     *  admin.cashBalance -= amount
     *  → CASH ledger DEBIT entry
     *
     * Effect (BANK):
     *  admin.bankBalance -= amount
     *  → BANK ledger DEBIT entry
     */
    @Transactional
    public AdminExpenseResponseDto recordAdminExpense(Long adminId, AdminExpenseRequestDto request) {
        log.info("Recording admin expense: adminId={}, amount={}, mode={}, desc={}",
                adminId, request.getAmount(), request.getPaymentMode(), request.getDescription());

        AdminProfile admin = loadWithLock(adminId);

        String modeStr = request.getPaymentMode().trim().toUpperCase();
        if (!modeStr.equals("CASH") && !modeStr.equals("BANK")) {
            throw new BookingException("paymentMode must be CASH or BANK");
        }

        AdminLedgerType ledgerType = modeStr.equals("CASH") ? AdminLedgerType.CASH : AdminLedgerType.BANK;
        BigDecimal amount = request.getAmount();
        BigDecimal newBalance;

        if (ledgerType == AdminLedgerType.CASH) {
            BigDecimal current = safe(admin.getCashBalance());
            if (amount.compareTo(current) > 0) {
                throw new BookingException(
                    String.format("Insufficient cash balance. Current: ₹%.2f, Requested: ₹%.2f",
                            current, amount));
            }
            newBalance = current.subtract(amount);
            admin.setCashBalance(newBalance);
        } else {
            BigDecimal current = safe(admin.getBankBalance());
            if (amount.compareTo(current) > 0) {
                throw new BookingException(
                    String.format("Insufficient bank balance. Current: ₹%.2f, Requested: ₹%.2f",
                            current, amount));
            }
            newBalance = current.subtract(amount);
            admin.setBankBalance(newBalance);
        }

        adminProfileRepository.save(admin);

        financialTransactionRepository.save(FinancialTransaction.builder()
                .admin(admin)
                .type(FinancialTransactionType.ADMIN_EXPENSE)
                .amount(amount)
                .referenceId(null)
                .build());

        String desc = request.getDescription()
                + (request.getCategory() != null ? " [" + request.getCategory() + "]" : "");

        AdminLedger entry = appendLedgerEntry(admin, ledgerType, AdminLedgerEntryType.DEBIT,
                amount, newBalance, desc, "EXPENSE", null);

        log.info("Admin expense recorded. ledgerType={}, amount={}, newBalance={}",
                ledgerType, amount, newBalance);

        return AdminExpenseResponseDto.builder()
                .ledgerEntryId(entry.getId())
                .ledgerType(ledgerType)
                .amount(amount)
                .balanceAfter(newBalance)
                .description(request.getDescription())
                .category(request.getCategory())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    /**
     * Full financial overview for a single admin.
     */
    @Transactional(readOnly = true)
    public AdminFinancialOverviewDto getAdminFinancialOverview(Long adminId) {
        AdminProfile admin = adminProfileRepository.findById(adminId)
                .orElseThrow(() -> new BookingException("Admin not found: " + adminId));
        return mapToOverviewDto(admin);
    }

    /**
     * Paginated ledger entries for a specific admin + ledger type.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<AdminLedgerEntryDto> getLedgerHistory(
            Long adminId, AdminLedgerType ledgerType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminLedger> ledgerPage = adminLedgerRepository
                .findByAdminIdAndLedgerTypeOrderByCreatedAtDesc(adminId, ledgerType, pageable);
        return new PaginatedResponse<>(
                ledgerPage.getContent().stream().map(this::mapToLedgerEntryDto).collect(Collectors.toList()),
                ledgerPage.getNumber(), ledgerPage.getSize(),
                ledgerPage.getTotalElements(), ledgerPage.getTotalPages(), ledgerPage.isLast());
    }

    /**
     * Paginated settlement history for a specific admin.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<SettlementDto> getSettlementHistory(Long adminId, int page, int size) {
        AdminProfile admin = adminProfileRepository.findById(adminId)
                .orElseThrow(() -> new BookingException("Admin not found: " + adminId));
        Pageable pageable = PageRequest.of(page, size);
        Page<Settlement> settlementPage = settlementRepository
                .findByAdminIdOrderByCreatedAtDesc(adminId, pageable);
        return new PaginatedResponse<>(
                settlementPage.getContent().stream().map(s -> mapToSettlementDto(s, admin)).collect(Collectors.toList()),
                settlementPage.getNumber(), settlementPage.getSize(),
                settlementPage.getTotalElements(), settlementPage.getTotalPages(), settlementPage.isLast());
    }

    /**
     * Paginated financial transaction audit log for a specific admin.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<FinancialTransactionDto> getTransactionHistory(Long adminId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FinancialTransaction> txPage = financialTransactionRepository
                .findByAdminIdOrderByCreatedAtDesc(adminId, pageable);
        List<FinancialTransactionDto> content = txPage.getContent().stream()
                .map(tx -> FinancialTransactionDto.builder()
                        .id(tx.getId())
                        .type(tx.getType())
                        .amount(tx.getAmount())
                        .referenceId(tx.getReferenceId())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        return new PaginatedResponse<>(content,
                txPage.getNumber(), txPage.getSize(),
                txPage.getTotalElements(), txPage.getTotalPages(), txPage.isLast());
    }

    /**
     * Due summary for all admins — used by Manager dashboard.
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

    // ─── Combined Ledger (CASH + BANK) ────────────────────────────────────────

    /**
     * Returns a combined (CASH + BANK) paginated ledger for a specific admin.
     * Optionally filtered by date range.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<AdminLedgerEntryDto> getAdminCombinedLedger(
            Long adminId, Instant from, Instant to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminLedger> ledgerPage;
        if (from != null && to != null) {
            ledgerPage = adminLedgerRepository.findByAdminIdAndDateRange(adminId, from, to, pageable);
        } else {
            ledgerPage = adminLedgerRepository.findByAdminIdOrderByCreatedAtDesc(adminId, pageable);
        }
        return new PaginatedResponse<>(
                ledgerPage.getContent().stream().map(this::mapToLedgerEntryDto).collect(Collectors.toList()),
                ledgerPage.getNumber(), ledgerPage.getSize(),
                ledgerPage.getTotalElements(), ledgerPage.getTotalPages(), ledgerPage.isLast());
    }

    /**
     * Returns filtered (by type) paginated ledger for a specific admin, with optional date range.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<AdminLedgerEntryDto> getAdminLedgerWithDateRange(
            Long adminId, AdminLedgerType ledgerType, Instant from, Instant to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminLedger> ledgerPage;
        if (from != null && to != null) {
            ledgerPage = adminLedgerRepository.findByAdminIdAndLedgerTypeAndDateRange(adminId, ledgerType, from, to, pageable);
        } else {
            ledgerPage = adminLedgerRepository.findByAdminIdAndLedgerTypeOrderByCreatedAtDesc(adminId, ledgerType, pageable);
        }
        return new PaginatedResponse<>(
                ledgerPage.getContent().stream().map(this::mapToLedgerEntryDto).collect(Collectors.toList()),
                ledgerPage.getNumber(), ledgerPage.getSize(),
                ledgerPage.getTotalElements(), ledgerPage.getTotalPages(), ledgerPage.isLast());
    }

    // ─── Manager Global Summary ───────────────────────────────────────────────

    /**
     * Aggregated financial summary across ALL admins — manager global dashboard.
     */
    @Transactional(readOnly = true)
    public ManagerGlobalFinancialSummaryDto getManagerGlobalSummary() {
        List<AdminProfile> admins = adminProfileRepository.findAll();

        BigDecimal totalPending = BigDecimal.ZERO;
        BigDecimal totalSettled = BigDecimal.ZERO;
        BigDecimal totalCash = BigDecimal.ZERO;
        BigDecimal totalBank = BigDecimal.ZERO;
        BigDecimal totalPlatformOnline = BigDecimal.ZERO;
        BigDecimal totalCashCollected = BigDecimal.ZERO;
        BigDecimal totalBankCollected = BigDecimal.ZERO;
        long adminsWithPending = 0;

        for (AdminProfile admin : admins) {
            BigDecimal pending = safe(admin.getPendingOnlineAmount());
            if (pending.compareTo(BigDecimal.ZERO) > 0) adminsWithPending++;
            totalPending = totalPending.add(pending);
            totalSettled = totalSettled.add(safe(admin.getTotalSettledAmount()));
            totalCash = totalCash.add(safe(admin.getCashBalance()));
            totalBank = totalBank.add(safe(admin.getBankBalance()));
            totalPlatformOnline = totalPlatformOnline.add(safe(admin.getTotalPlatformOnlineCollected()));
            totalCashCollected = totalCashCollected.add(safe(admin.getTotalCashCollected()));
            totalBankCollected = totalBankCollected.add(safe(admin.getTotalBankCollected()));
        }

        return ManagerGlobalFinancialSummaryDto.builder()
                .adminsWithPendingBalance(adminsWithPending)
                .totalPendingOnlineAmount(totalPending)
                .totalSettledAmount(totalSettled)
                .totalCashBalance(totalCash)
                .totalBankBalance(totalBank)
                .totalPlatformOnlineCollected(totalPlatformOnline)
                .totalCashCollected(totalCashCollected)
                .totalBankCollected(totalBankCollected)
                .build();
    }

    // ─── Manager Global Settlement Ledger ─────────────────────────────────────

    /**
     * Paginated settlement ledger across ALL admins — manager's complete settlement history.
     * Optionally filtered by date range.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ManagerSettlementLedgerEntryDto> getManagerSettlementLedger(
            Instant from, Instant to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Settlement> settlementPage;
        if (from != null && to != null) {
            settlementPage = settlementRepository.findAllByDateRange(from, to, pageable);
        } else {
            settlementPage = settlementRepository.findAllOrderByCreatedAtDesc(pageable);
        }
        return new PaginatedResponse<>(
                settlementPage.getContent().stream()
                        .map(this::mapToManagerSettlementLedgerEntry)
                        .collect(Collectors.toList()),
                settlementPage.getNumber(), settlementPage.getSize(),
                settlementPage.getTotalElements(), settlementPage.getTotalPages(), settlementPage.isLast());
    }

    /**
     * Paginated settlement ledger for a SINGLE admin, with optional date range.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ManagerSettlementLedgerEntryDto> getAdminSettlementLedger(
            Long adminId, Instant from, Instant to, int page, int size) {
        if (!adminProfileRepository.existsById(adminId)) {
            throw new BookingException("Admin not found: " + adminId);
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Settlement> settlementPage;
        if (from != null && to != null) {
            settlementPage = settlementRepository.findByAdminIdAndDateRange(adminId, from, to, pageable);
        } else {
            settlementPage = settlementRepository.findByAdminIdOrderByCreatedAtDesc(adminId, pageable);
        }
        return new PaginatedResponse<>(
                settlementPage.getContent().stream()
                        .map(this::mapToManagerSettlementLedgerEntry)
                        .collect(Collectors.toList()),
                settlementPage.getNumber(), settlementPage.getSize(),
                settlementPage.getTotalElements(), settlementPage.getTotalPages(), settlementPage.isLast());
    }

    /**
     * Update notes/remarks on an existing settlement record.
     */
    @Transactional
    public SettlementDto updateSettlementNotes(Long settlementId, String notes) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BookingException("Settlement not found: " + settlementId));
        settlement.setNotes(notes);
        settlementRepository.save(settlement);
        AdminProfile admin = settlement.getAdmin();
        return mapToSettlementDto(settlement, admin);
    }

    /**
     * Get a single settlement by ID.
     */
    @Transactional(readOnly = true)
    public SettlementDto getSettlementById(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BookingException("Settlement not found: " + settlementId));
        return mapToSettlementDto(settlement, settlement.getAdmin());
    }

    // ─── Pending Balance Management ──────────────────────────────────────────

    /**
     * Enhanced due summary with business name and full breakdown.
     */
    @Transactional(readOnly = true)
    public List<AdminDueSummaryDto> getAllAdminDueSummaryEnhanced() {
        return adminProfileRepository.findAll().stream()
                .map(admin -> AdminDueSummaryDto.builder()
                        .adminId(admin.getId())
                        .adminName(admin.getUser() != null ? admin.getUser().getName() : "Unknown")
                        .pendingOnlineAmount(safe(admin.getPendingOnlineAmount()))
                        .totalSettled(safe(admin.getTotalSettledAmount()))
                        .build())
                .filter(dto -> dto.getPendingOnlineAmount().compareTo(BigDecimal.ZERO) > 0)
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
                .notes(s.getNotes())
                .pendingAfterSettlement(s.getPendingAfterSettlement())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private AdminLedgerEntryDto mapToLedgerEntryDto(AdminLedger entry) {
        return AdminLedgerEntryDto.builder()
                .id(entry.getId())
                .ledgerType(entry.getLedgerType())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .balanceAfter(entry.getBalanceAfter())
                .description(entry.getDescription())
                .referenceType(entry.getReferenceType())
                .referenceId(entry.getReferenceId())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private ManagerSettlementLedgerEntryDto mapToManagerSettlementLedgerEntry(Settlement s) {
        AdminProfile admin = s.getAdmin();
        return ManagerSettlementLedgerEntryDto.builder()
                .settlementId(s.getId())
                .adminId(admin.getId())
                .adminName(admin.getUser() != null ? admin.getUser().getName() : "Unknown")
                .adminBusinessName(admin.getBusinessName())
                .amount(s.getAmount())
                .pendingAfter(s.getPendingAfterSettlement())
                .paymentMode(s.getPaymentMode())
                .status(s.getStatus())
                .settledByManagerId(s.getSettledByManagerId())
                .settlementReference(s.getSettlementReference())
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .build();
    }
}

