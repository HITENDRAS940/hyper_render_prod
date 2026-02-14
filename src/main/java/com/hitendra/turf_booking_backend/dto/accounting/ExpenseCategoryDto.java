package com.hitendra.turf_booking_backend.dto.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.ExpenseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategoryDto {

    private Long id;
    private Long adminProfileId;
    private String name;
    private ExpenseType type;
    private Instant createdAt;
}

