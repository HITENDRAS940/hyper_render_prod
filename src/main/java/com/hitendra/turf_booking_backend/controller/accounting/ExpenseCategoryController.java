package com.hitendra.turf_booking_backend.controller.accounting;

import com.hitendra.turf_booking_backend.dto.accounting.CreateExpenseCategoryRequest;
import com.hitendra.turf_booking_backend.dto.accounting.ExpenseCategoryDto;
import com.hitendra.turf_booking_backend.entity.accounting.ExpenseCategory;
import com.hitendra.turf_booking_backend.entity.accounting.ExpenseType;
import com.hitendra.turf_booking_backend.service.accounting.ExpenseCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/accounting/expense-categories")
@RequiredArgsConstructor
@Tag(name = "Expense Categories", description = "Manage expense categories (Admin only)")
@PreAuthorize("hasRole('ADMIN')")
public class ExpenseCategoryController {

    private final ExpenseCategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create expense category",
               description = "Create a new expense category for current admin. Category names must be unique per admin.")
    public ResponseEntity<ExpenseCategoryDto> createCategory(@Valid @RequestBody CreateExpenseCategoryRequest request) {
        ExpenseCategory category = categoryService.createCategory(request);
        return ResponseEntity.ok(mapToDto(category));
    }

    @GetMapping
    @Operation(summary = "Get my expense categories",
               description = "Get all expense categories for the current admin")
    public ResponseEntity<List<ExpenseCategoryDto>> getAllCategories() {
        List<ExpenseCategory> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories.stream().map(this::mapToDto).collect(Collectors.toList()));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get my categories by type",
               description = "Get expense categories by type (FIXED or VARIABLE) for the current admin")
    public ResponseEntity<List<ExpenseCategoryDto>> getCategoriesByType(@PathVariable ExpenseType type) {
        List<ExpenseCategory> categories = categoryService.getCategoriesByType(type);
        return ResponseEntity.ok(categories.stream().map(this::mapToDto).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID",
               description = "Get expense category details by ID. Only accessible if it belongs to current admin.")
    public ResponseEntity<ExpenseCategoryDto> getCategoryById(@PathVariable Long id) {
        ExpenseCategory category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(mapToDto(category));
    }

    private ExpenseCategoryDto mapToDto(ExpenseCategory category) {
        return ExpenseCategoryDto.builder()
            .id(category.getId())
            .adminProfileId(category.getAdminProfile().getId())
            .name(category.getName())
            .type(category.getType())
            .createdAt(category.getCreatedAt())
            .build();
    }
}

