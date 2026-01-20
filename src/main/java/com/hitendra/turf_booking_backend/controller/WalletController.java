package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.wallet.*;
import com.hitendra.turf_booking_backend.entity.WalletTransaction;
import com.hitendra.turf_booking_backend.service.WalletService;
import com.hitendra.turf_booking_backend.service.WalletPaymentService;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for wallet operations.
 * All balance-changing operations are handled by the service layer with proper locking.
 */
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wallet", description = "User wallet management APIs")
public class WalletController {

    private final WalletService walletService;
    private final WalletPaymentService walletPaymentService;
    private final AuthUtil authUtil;

    // ==================== User Wallet Operations ====================

    @GetMapping
    @Operation(summary = "Get wallet", description = "Get current user's wallet details")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<WalletDto> getWallet() {
        Long userId = authUtil.getCurrentUserId();
        WalletDto wallet = walletService.getWalletDto(userId);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/balance")
    @Operation(summary = "Get balance", description = "Get current user's wallet balance")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<WalletBalanceResponse> getBalance() {
        Long userId = authUtil.getCurrentUserId();
        var balance = walletService.getBalance(userId);
        return ResponseEntity.ok(WalletBalanceResponse.builder()
                .balance(balance)
                .currency("INR")
                .build());
    }

    @PostMapping("/topup")
    @Operation(summary = "Initiate top-up", description = "Create a payment order for wallet top-up")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<WalletTopupResponse> initiateTopup(@Valid @RequestBody WalletTopupRequest request) {
        Long userId = authUtil.getCurrentUserId();
        log.info("Wallet top-up requested: userId={}, amount={}", userId, request.getAmount());

        WalletTopupResponse response = walletPaymentService.initiateTopup(userId, request.getAmount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get transactions", description = "Get paginated transaction history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<Page<WalletTransactionDto>> getTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = authUtil.getCurrentUserId();
        Page<WalletTransactionDto> transactions = walletService.getTransactionHistory(userId, pageable);
        return ResponseEntity.ok(transactions);
    }

    // ==================== Admin Operations ====================

    @GetMapping("/admin/user/{userId}")
    @Operation(summary = "Get user wallet (Admin)", description = "Get wallet details for a specific user")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<WalletDto> getUserWallet(@PathVariable Long userId) {
        WalletDto wallet = walletService.getWalletDto(userId);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/admin/credit")
    @Operation(summary = "Admin credit", description = "Credit amount to user's wallet (admin only)")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<WalletTransactionDto> adminCredit(
            @Valid @RequestBody AdminWalletOperationRequest request) {
        String adminId = authUtil.getCurrentUserId().toString();
        log.info("Admin credit: adminId={}, userId={}, amount={}", adminId, request.getUserId(), request.getAmount());

        WalletTransaction tx = walletService.adminCredit(
                request.getUserId(),
                request.getAmount(),
                request.getReason(),
                adminId);

        return ResponseEntity.ok(toDto(tx));
    }

    @PostMapping("/admin/debit")
    @Operation(summary = "Admin debit", description = "Debit amount from user's wallet (admin only)")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<WalletTransactionDto> adminDebit(
            @Valid @RequestBody AdminWalletOperationRequest request) {
        String adminId = authUtil.getCurrentUserId().toString();
        log.info("Admin debit: adminId={}, userId={}, amount={}", adminId, request.getUserId(), request.getAmount());

        WalletTransaction tx = walletService.adminDebit(
                request.getUserId(),
                request.getAmount(),
                request.getReason(),
                adminId);

        return ResponseEntity.ok(toDto(tx));
    }

    @PostMapping("/admin/block/{walletId}")
    @Operation(summary = "Block wallet", description = "Block a user's wallet")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> blockWallet(@PathVariable Long walletId, @RequestParam String reason) {
        log.info("Blocking wallet: walletId={}, reason={}", walletId, reason);
        walletService.blockWallet(walletId, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/unblock/{walletId}")
    @Operation(summary = "Unblock wallet", description = "Unblock a user's wallet")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> unblockWallet(@PathVariable Long walletId) {
        log.info("Unblocking wallet: walletId={}", walletId);
        walletService.unblockWallet(walletId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/transactions/{userId}")
    @Operation(summary = "Get user transactions (Admin)", description = "Get transaction history for a specific user")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<WalletTransactionDto>> getUserTransactions(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WalletTransactionDto> transactions = walletService.getTransactionHistory(userId, pageable);
        return ResponseEntity.ok(transactions);
    }

    // ==================== DTO Converter ====================

    private WalletTransactionDto toDto(WalletTransaction tx) {
        return WalletTransactionDto.builder()
                .id(tx.getId())
                .walletId(tx.getWallet().getId())
                .amount(tx.getAmount())
                .type(tx.getType())
                .source(tx.getSource())
                .referenceId(tx.getReferenceId())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}

