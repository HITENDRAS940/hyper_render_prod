# Query Fixes Applied

## Issue 1: ResourcePriceRuleRepository
**Error**: `Could not resolve attribute 'pricePerSlot'`

**Root Cause**: Query referenced non-existent field `pricePerSlot`

**Fix**: Updated to use correct entity fields:
- Changed `pricePerSlot` to `basePrice`
- Added new method `findApplicablePriceComponents()` to fetch both `basePrice` and `extraCharge`

**File**: `/repository/ResourcePriceRuleRepository.java`

---

## Issue 2: InventorySaleRepository
**Error**: `Could not resolve attribute 'quantity' of 'InventorySale'`

**Root Cause**: Query tried to access `quantity` directly on `InventorySale`, but quantity is in the related `InventorySaleItem` entity

**Fix**: Updated query to join through the relationship:
```java
// Before (incorrect)
SELECT COALESCE(SUM(s.quantity), 0) FROM InventorySale s WHERE s.item.id = :itemId

// After (correct)
SELECT COALESCE(SUM(si.quantity), 0) FROM InventorySaleItem si WHERE si.item.id = :itemId AND si.sale.saleDate BETWEEN :startDate AND :endDate
```

**File**: `/repository/accounting/InventorySaleRepository.java`

---

## Entity Relationships Reference

### InventorySale Structure
```
InventorySale
├── id
├── service
├── totalAmount
├── paymentMode
├── saleDate
└── items: List<InventorySaleItem>  ← One-to-Many
```

### InventorySaleItem Structure
```
InventorySaleItem
├── id
├── sale (→ InventorySale)
├── item (→ InventoryItem)
├── quantity  ← This is where quantity lives!
├── sellingPrice
└── lineTotal
```

### ResourcePriceRule Structure
```
ResourcePriceRule
├── id
├── resourceSlotConfig
├── dayType
├── startTime
├── endTime
├── basePrice  ← Used instead of pricePerSlot
├── extraCharge  ← Additional charge
├── reason
├── priority
└── enabled
```

---

## Compilation Status
✅ All errors resolved
✅ Project compiles successfully
✅ All repositories validated by Spring Data JPA

---

## Next Steps
1. Set up environment variables (JWT_SECRET, DATABASE_URL, etc.)
2. Run the application with proper configuration
3. Test the optimized queries with real data
4. Monitor query performance improvements
