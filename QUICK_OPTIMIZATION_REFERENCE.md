# Quick Reference: Using Optimized Queries

## When Writing New Service Methods

### ❌ **AVOID: Loading All Entities**
```java
// BAD - Loads all services from database
public List<ServiceDto> getServicesByCity(String city) {
    return serviceRepository.findAll().stream()
        .filter(s -> s.getCity().equalsIgnoreCase(city))
        .map(this::convertToDto)
        .collect(Collectors.toList());
}
```

### ✅ **USE: Database-Level Filtering**
```java
// GOOD - Filters at database level
public List<ServiceDto> getServicesByCity(String city) {
    return serviceRepository.findByCityIgnoreCase(city).stream()
        .map(this::convertToDto)
        .collect(Collectors.toList());
}
```

---

## Projection vs Full Entity

### ❌ **AVOID: Full Entity for List Views**
```java
// BAD - Loads unnecessary data
public PaginatedResponse<ServiceCardDto> getAllServices(int page, int size) {
    Page<Service> services = serviceRepository.findAll(pageable);
    // Loads: images, description, activities, amenities, etc.
}
```

### ✅ **USE: Projections for List Views**
```java
// GOOD - Only loads required fields
public PaginatedResponse<ServiceCardDto> getAllServices(int page, int size) {
    Page<ServiceCardProjection> services = 
        serviceRepository.findAllServicesCardProjected(pageable);
    // Loads: id, name, location, city, availability only
}
```

---

## Existence Checks

### ❌ **AVOID: Loading Entity to Check Existence**
```java
// BAD - Loads full entity
public boolean canUserModifyService(Long serviceId, Long adminId) {
    try {
        Service service = serviceRepository.findById(serviceId).orElse(null);
        return service != null && service.getCreatedBy().getId().equals(adminId);
    } catch (Exception e) {
        return false;
    }
}
```

### ✅ **USE: Existence Query**
```java
// GOOD - Single COUNT query
public boolean canUserModifyService(Long serviceId, Long adminId) {
    return serviceRepository.existsByIdAndCreatedById(serviceId, adminId);
}
```

---

## Getting Single Fields

### ❌ **AVOID: Loading Full Entity for One Field**
```java
// BAD - Loads entire service
public String getServiceName(Long serviceId) {
    Service service = serviceRepository.findById(serviceId)
        .orElseThrow(() -> new RuntimeException("Not found"));
    return service.getName();
}
```

### ✅ **USE: Single Field Query**
```java
// GOOD - Only fetches name column
public String getServiceName(Long serviceId) {
    return serviceRepository.findServiceNameById(serviceId)
        .orElseThrow(() -> new RuntimeException("Not found"));
}
```

---

## Batch Operations

### ❌ **AVOID: Loading All Entities for IDs**
```java
// BAD - Loads all data just to get IDs
public List<Long> getServiceIdsForAdmin(Long adminId) {
    return serviceRepository.findByCreatedById(adminId).stream()
        .map(Service::getId)
        .collect(Collectors.toList());
}
```

### ✅ **USE: ID-Only Query**
```java
// GOOD - Only fetches ID column
public List<Long> getServiceIdsForAdmin(Long adminId) {
    return serviceRepository.findServiceIdsByCreatedById(adminId);
}
```

---

## Counting Records

### ❌ **AVOID: Loading to Count**
```java
// BAD - Loads all records
public long countEnabledRules(Long resourceId) {
    return priceRuleRepository.findEnabledRulesByResourceId(resourceId).size();
}
```

### ✅ **USE: COUNT Query**
```java
// GOOD - Single COUNT query
public long countEnabledRules(Long resourceId) {
    return priceRuleRepository.countEnabledRulesByResourceId(resourceId);
}
```

---

## DISTINCT Values

### ❌ **AVOID: Java Stream DISTINCT**
```java
// BAD - Loads all services
public List<String> getAllCities() {
    return serviceRepository.findAll().stream()
        .map(Service::getCity)
        .filter(Objects::nonNull)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
}
```

### ✅ **USE: Database DISTINCT**
```java
// GOOD - Database-level DISTINCT
public List<String> getAllCities() {
    return serviceRepository.findAllDistinctCities();
}
```

---

## Price Calculations

### When Full Rule Entity Needed
```java
// Use when you need multiple rule properties
List<ResourcePriceRule> rules = priceRuleRepository.findApplicableRules(
    resourceId, dayType, slotTime);

for (ResourcePriceRule rule : rules) {
    log.info("Applying rule: {}, reason: {}", rule.getId(), rule.getReason());
    // Use basePrice, extraCharge, priority, etc.
}
```

### When Only Price Values Needed
```java
// Use when you only need the price numbers
List<Object[]> priceComponents = priceRuleRepository
    .findApplicablePriceComponentsList(resourceId, dayType, slotTime);

if (!priceComponents.isEmpty()) {
    Object[] price = priceComponents.get(0);
    Double basePrice = (Double) price[0];
    Double extraCharge = (Double) price[1];
    return basePrice + extraCharge;
}
```

---

## Repository Method Naming Convention

### Standard Patterns to Use

1. **Find Projection**: `find{EntityName}Projected`
   ```java
   Page<ServiceCardProjection> findAllServicesCardProjected(Pageable pageable);
   ```

2. **Find IDs Only**: `find{EntityName}IdsBy{Criteria}`
   ```java
   List<Long> findServiceIdsByCreatedById(Long adminId);
   ```

3. **Exists Check**: `existsBy{Criteria}`
   ```java
   boolean existsByIdAndCreatedById(Long serviceId, Long adminId);
   ```

4. **Count**: `countBy{Criteria}`
   ```java
   long countByServiceIdAndDateRange(Long serviceId, LocalDate start, LocalDate end);
   ```

5. **Single Field**: `find{FieldName}By{Criteria}`
   ```java
   Optional<String> findServiceNameById(Long serviceId);
   ```

---

## Common Patterns by Use Case

### 1. List View API Endpoint
```java
@GetMapping("/services")
public ResponseEntity<PaginatedResponse<ServiceCardDto>> getServices(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    
    // Use projection for list view
    Page<ServiceCardProjection> projections = 
        serviceRepository.findAllServicesCardProjected(PageRequest.of(page, size));
    
    List<ServiceCardDto> dtos = projections.stream()
        .map(p -> new ServiceCardDto(p.getId(), p.getName(), p.getLocation(), 
                                     p.getAvailability()))
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(new PaginatedResponse<>(dtos, ...));
}
```

### 2. Detail View API Endpoint
```java
@GetMapping("/services/{id}")
public ResponseEntity<ServiceDto> getService(@PathVariable Long id) {
    // Load full entity for detail view
    Service service = serviceRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
    
    return ResponseEntity.ok(convertToDto(service));
}
```

### 3. Permission Check Before Modification
```java
@PutMapping("/services/{id}")
public ResponseEntity<ServiceDto> updateService(
        @PathVariable Long id,
        @RequestBody ServiceUpdateRequest request,
        @AuthenticationPrincipal User user) {
    
    // Quick permission check
    if (!serviceRepository.existsByIdAndCreatedById(id, user.getAdminProfile().getId())) {
        throw new ForbiddenException("Cannot modify this service");
    }
    
    // Now load for modification
    Service service = serviceRepository.findById(id).orElseThrow();
    service.setName(request.getName());
    // ... update fields
    serviceRepository.save(service);
    
    return ResponseEntity.ok(convertToDto(service));
}
```

### 4. Batch Operation
```java
public void processServicesInCity(String city) {
    // Get IDs only
    List<Long> serviceIds = serviceRepository.findAvailableServiceIdsByCity(city);
    
    // Process in batches
    for (Long id : serviceIds) {
        // Load full entity only when needed for modification
        Service service = serviceRepository.findById(id).orElseThrow();
        processService(service);
        serviceRepository.save(service);
    }
}
```

### 5. Statistics/Dashboard
```java
public DashboardStats getAdminDashboard(Long adminId) {
    // Use optimized queries for statistics
    long totalServices = serviceRepository.countByCreatedById(adminId);
    List<String> cities = serviceRepository.findDistinctCitiesByAdminId(adminId);
    long enabledRules = priceRuleRepository.countEnabledRulesForAdmin(adminId);
    
    return DashboardStats.builder()
        .totalServices(totalServices)
        .cities(cities)
        .totalPriceRules(enabledRules)
        .build();
}
```

---

## Performance Checklist

Before committing new repository methods, verify:

- [ ] Does the query filter at database level (not in Java)?
- [ ] For list views, am I using projections instead of full entities?
- [ ] For existence checks, am I using `exists` queries?
- [ ] For counts, am I using `COUNT` queries?
- [ ] For single fields, am I querying only that field?
- [ ] Am I avoiding N+1 queries by using JOINs or @EntityGraph?
- [ ] Are my @Query annotations using proper JPQL (no LIMIT, proper JOINs)?

---

## Testing Your Optimizations

### Enable Query Logging
```properties
# application-dev.properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### Check Query Output
```sql
-- BAD: Fetches all columns
SELECT * FROM services WHERE city = ?

-- GOOD: Only required columns
SELECT s.id, s.name, s.location, s.city, s.availability 
FROM services s WHERE city = ?
```

### Measure Performance
```java
@Slf4j
public class ServiceService {
    public List<String> getAvailableCities() {
        StopWatch watch = new StopWatch();
        watch.start();
        
        List<String> cities = serviceRepository.findAllDistinctCities();
        
        watch.stop();
        log.info("Fetched {} cities in {}ms", cities.size(), watch.getTotalTimeMillis());
        
        return cities;
    }
}
```

---

## Common Mistakes to Avoid

### 1. Using `findAll()` When You Need Filtered Data
```java
// ❌ DON'T DO THIS
List<Service> allServices = serviceRepository.findAll();
List<Service> filtered = allServices.stream()
    .filter(s -> s.getCity().equals("Mumbai"))
    .collect(Collectors.toList());

// ✅ DO THIS
List<Service> filtered = serviceRepository.findByCityIgnoreCase("Mumbai");
```

### 2. Loading Entity Just to Get ID
```java
// ❌ DON'T DO THIS
Service service = serviceRepository.findById(id).orElseThrow();
Long serviceId = service.getId(); // You already have the ID!

// ✅ DO THIS
Long serviceId = id; // Just use the ID you already have
```

### 3. Loading Full Entities in Loops
```java
// ❌ DON'T DO THIS
for (Long id : serviceIds) {
    Service service = serviceRepository.findById(id).orElseThrow();
    System.out.println(service.getName()); // Only need name
}

// ✅ DO THIS
List<String> names = serviceRepository.findServiceNamesByIds(serviceIds);
```

### 4. Using LIMIT in JPQL
```java
// ❌ DON'T DO THIS
@Query("SELECT s FROM Service s ORDER BY s.createdAt DESC LIMIT 10")
List<Service> findRecent();

// ✅ DO THIS
@Query("SELECT s FROM Service s ORDER BY s.createdAt DESC")
List<Service> findRecent();
// Then use: repository.findRecent().stream().limit(10)
// OR use Pageable: repository.findAll(PageRequest.of(0, 10))
```

---

## Summary

**Golden Rule**: Only load what you need, when you need it.

- **List views** → Use projections
- **Detail views** → Load full entity
- **Existence checks** → Use exists queries
- **Counts** → Use COUNT queries
- **Single fields** → Query only that field
- **Batch operations** → Get IDs first, load selectively

Following these patterns will keep your backend fast and responsive! 🚀
