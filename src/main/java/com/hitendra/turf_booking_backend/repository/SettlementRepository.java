package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Settlement;
import com.hitendra.turf_booking_backend.entity.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByAdminIdOrderByCreatedAtDesc(Long adminId);

    List<Settlement> findByAdminIdAndStatusOrderByCreatedAtDesc(Long adminId, SettlementStatus status);

    Page<Settlement> findByAdminIdOrderByCreatedAtDesc(Long adminId, Pageable pageable);
}

