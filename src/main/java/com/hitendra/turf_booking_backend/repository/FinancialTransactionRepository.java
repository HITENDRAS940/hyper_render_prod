package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.FinancialTransaction;
import com.hitendra.turf_booking_backend.entity.FinancialTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {

    List<FinancialTransaction> findByAdminIdOrderByCreatedAtDesc(Long adminId);

    List<FinancialTransaction> findByAdminIdAndTypeOrderByCreatedAtDesc(Long adminId, FinancialTransactionType type);
}

