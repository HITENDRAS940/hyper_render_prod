package com.hitendra.turf_booking_backend.config;

import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.entity.accounting.ExpenseCategory;
import com.hitendra.turf_booking_backend.entity.accounting.ExpenseType;
import com.hitendra.turf_booking_backend.repository.*;
import com.hitendra.turf_booking_backend.repository.accounting.ExpenseCategoryRepository;
import com.hitendra.turf_booking_backend.service.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DataInitializer - Creates test data for local development
 * Enabled for local development - creates sample services and users
 */
@Component
@Lazy(false)  // Force eager initialization even with lazy-initialization=true
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceResourceRepository serviceResourceRepository;
    private final ResourceSlotConfigRepository resourceSlotConfigRepository;
    private final ResourcePriceRuleRepository resourcePriceRuleRepository;
    private final ActivityRepository activityRepository;
    private final UserRegistrationService userRegistrationService;
    private final ExpenseCategoryRepository expenseCategoryRepository;

    @Override
    public void run(String... args) {
        log.info("🚀 DataInitializer started...");

        try {
            // Initialize activities (required for system)
            if (activityRepository.count() == 0) {
                log.info("📋 No activities found, initializing...");
                initializeActivities();
            } else {
                log.info("✅ Activities already initialized (count: {})", activityRepository.count());
            }

            // NOTE: Expense categories are now admin-specific (after V12 migration)
            // They will be created automatically when admins create their services/expenses
            // Or they can be created via the expense category API endpoint

            // Create manager user
            if (userRepository.findByEmail("gethyperadmin@gmail.com").isEmpty()) {
                log.info("👤 Manager user not found, creating...");
                initializeManagerUser();
            } else {
                log.info("✅ Manager user already exists");
            }

            // Add minimal test service in Mumbai
            if (serviceRepository.count() == 0) {
                log.info("🏟️ No services found, creating test services...");
                initializeMinimalTestService();
            } else {
                log.info("✅ Services already exist (count: {})", serviceRepository.count());
            }

            log.info("🎉 DataInitializer completed successfully!");

            // COMMENTED OUT: Full sample data initialization
            // if (userRepository.count() == 0) {
            //     initializeSampleData();
            // }

            // COMMENTED OUT: Weekend price rule
            // addWeekendExtraPriceRuleForResource(1L, 150.0);

        } catch (Exception e) {
            log.error("❌ FATAL ERROR in DataInitializer: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize data", e);
        }
    }

    /**
     * Initialize test services in Mumbai and Vellore with proper configuration
     */
    private void initializeMinimalTestService() {
        try {
            log.info("🏗️ Creating test services in Mumbai and Vellore...");

            // Get activities
            Activity cricket = activityRepository.findByCode("CRICKET")
                    .orElseThrow(() -> new RuntimeException("Cricket activity not found"));
            Activity football = activityRepository.findByCode("FOOTBALL")
                    .orElseThrow(() -> new RuntimeException("Football activity not found"));

            // Create Mumbai Admin
            User mumbaiAdmin = User.builder()
                    .email("mumbai.admin@hyper.com")
                    .phone("+919876543210")
                    .name("Mumbai Admin")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            mumbaiAdmin = userRepository.save(mumbaiAdmin);
            log.info("✅ Created Mumbai admin: {}", mumbaiAdmin.getEmail());

            AdminProfile mumbaiProfile = AdminProfile.builder()
                    .user(mumbaiAdmin)
                    .businessName("Mumbai Sports Arena")
                    .businessAddress("Andheri West, Mumbai, Maharashtra")
                    .city("Mumbai")
                    .gstNumber("27AABCT1234F1Z5")
                    .build();
            mumbaiProfile = adminProfileRepository.save(mumbaiProfile);

            // Create expense categories for Mumbai admin
            createExpenseCategoriesForAdmin(mumbaiProfile);
            log.info("✅ Created expense categories for Mumbai admin");

            // Create Vellore Admin
            User velloreAdmin = User.builder()
                    .email("vellore.admin@hyper.com")
                    .phone("+919876543211")
                    .name("Vellore Admin")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            velloreAdmin = userRepository.save(velloreAdmin);
            log.info("✅ Created Vellore admin: {}", velloreAdmin.getEmail());

            AdminProfile velloreProfile = AdminProfile.builder()
                    .user(velloreAdmin)
                    .businessName("Vellore Sports Complex")
                    .businessAddress("Katpadi, Vellore, Tamil Nadu")
                    .city("Vellore")
                    .gstNumber("33AABCV5678G1Z9")
                    .build();
            velloreProfile = adminProfileRepository.save(velloreProfile);

            // Create expense categories for Vellore admin
            createExpenseCategoriesForAdmin(velloreProfile);
            log.info("✅ Created expense categories for Vellore admin");

            // === MUMBAI SERVICE ===
            Service mumbaiService = Service.builder()
                    .name("Mumbai Premium Sports Arena")
                    .description("State-of-the-art cricket and football facility with international standard turfs, floodlights, and professional coaching available")
                    .location("Andheri West, Mumbai")
                    .city("Mumbai")
                    .latitude(19.1136)
                    .longitude(72.8697)
                    .contactNumber("+919876543210")
                    .startTime(LocalTime.of(6, 0))
                    .endTime(LocalTime.of(23, 0))
                    .createdBy(mumbaiProfile)
                    .availability(true)
                    .amenities(List.of("Floodlights", "Parking", "Changing Rooms", "Washrooms", "Drinking Water", "First Aid", "Equipment Rental"))
                    .images(List.of(
                            "https://images.unsplash.com/photo-1529900748604-07564a03e7a6?w=800",
                            "https://images.unsplash.com/photo-1575361204480-aadea25e6e68?w=800",
                            "https://images.unsplash.com/photo-1518605348416-72580200d3f8?w=800"
                    ))
                    .activities(List.of(cricket, football))
                    .build();
            mumbaiService = serviceRepository.save(mumbaiService);
            log.info("✅ Created Mumbai service: {}", mumbaiService.getName());

            createResourcesForService(mumbaiService, "Mumbai Turf", 2, 1200.0);

            // === VELLORE SERVICE ===
            Service velloreService = Service.builder()
                    .name("Vellore Champions Sports Complex")
                    .description("Premium multi-sport facility featuring cricket nets, football turf, and practice grounds. Perfect for tournaments, training sessions, and recreational play. Located near VIT University")
                    .location("Katpadi, Vellore")
                    .city("Vellore")
                    .latitude(12.9716)
                    .longitude(79.1579)
                    .contactNumber("+919876543211")
                    .startTime(LocalTime.of(5, 30))
                    .endTime(LocalTime.of(23, 30))
                    .createdBy(velloreProfile)
                    .availability(true)
                    .amenities(List.of("Floodlights", "Free Parking", "Changing Rooms", "Washrooms", "Cafeteria", "Seating Area", "Equipment Rental", "Coaching Available"))
                    .images(List.of(
                            "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=800",
                            "https://images.unsplash.com/photo-1593341646782-e0b495cffd32?w=800",
                            "https://images.unsplash.com/photo-1624526267942-ab0ff8a3e972?w=800"
                    ))
                    .activities(List.of(cricket, football))
                    .build();
            velloreService = serviceRepository.save(velloreService);
            log.info("✅ Created Vellore service: {}", velloreService.getName());

            createResourcesForService(velloreService, "Vellore Ground", 3, 800.0);

            log.info("🎉 Test services initialized successfully!");

        } catch (Exception e) {
            log.error("❌ Failed to initialize test services", e);
        }
    }

    /**
     * Create resources with comprehensive slot config and price rules
     */
    private void createResourcesForService(Service service, String resourcePrefix, int count, double basePrice) {
        for (int i = 1; i <= count; i++) {
            ServiceResource resource = ServiceResource.builder()
                    .service(service)
                    .name(resourcePrefix + " " + i)
                    .description("Professional " + resourcePrefix.toLowerCase() + " " + i + " with excellent drainage and maintenance")
                    .enabled(true)
                    .activities(new ArrayList<>(service.getActivities()))  // Add all service activities to resource
                    .build();
            resource = serviceResourceRepository.save(resource);
            log.info("✅ Created resource: {} with {} activities", resource.getName(), resource.getActivities().size());

            // Create slot config
            ResourceSlotConfig slotConfig = ResourceSlotConfig.builder()
                    .resource(resource)
                    .openingTime(service.getStartTime())
                    .closingTime(service.getEndTime())
                    .slotDurationMinutes(60)
                    .basePrice(basePrice)
                    .enabled(true)
                    .build();
            slotConfig = resourceSlotConfigRepository.save(slotConfig);
            log.info("✅ Created slot config for {}", resource.getName());

            // Price Rule 1: Standard weekday pricing (6 AM - 6 PM)
            ResourcePriceRule weekdayDayRule = ResourcePriceRule.builder()
                    .resourceSlotConfig(slotConfig)
                    .dayType(DayType.WEEKDAY)
                    .startTime(LocalTime.of(6, 0))
                    .endTime(LocalTime.of(18, 0))
                    .basePrice(basePrice)
                    .extraCharge(0.0)
                    .reason("Standard weekday daytime pricing")
                    .priority(1)
                    .enabled(true)
                    .build();
            resourcePriceRuleRepository.save(weekdayDayRule);

            // Price Rule 2: Weekday evening premium (6 PM - 11 PM) - Peak hours
            ResourcePriceRule weekdayEveningRule = ResourcePriceRule.builder()
                    .resourceSlotConfig(slotConfig)
                    .dayType(DayType.WEEKDAY)
                    .startTime(LocalTime.of(18, 0))
                    .endTime(LocalTime.of(23, 0))
                    .basePrice(basePrice)
                    .extraCharge(basePrice * 0.30) // 30% extra
                    .reason("Peak evening hours - High demand")
                    .priority(2)
                    .enabled(true)
                    .build();
            resourcePriceRuleRepository.save(weekdayEveningRule);

            // Price Rule 3: Weekend all-day premium
            ResourcePriceRule weekendRule = ResourcePriceRule.builder()
                    .resourceSlotConfig(slotConfig)
                    .dayType(DayType.WEEKEND)
                    .startTime(LocalTime.of(6, 0))
                    .endTime(LocalTime.of(23, 0))
                    .basePrice(basePrice)
                    .extraCharge(basePrice * 0.50) // 50% extra
                    .reason("Weekend premium - Tournament pricing")
                    .priority(3)
                    .enabled(true)
                    .build();
            resourcePriceRuleRepository.save(weekendRule);

            // Price Rule 4: Early morning discount (5:30 AM - 8 AM)
            ResourcePriceRule earlyMorningRule = ResourcePriceRule.builder()
                    .resourceSlotConfig(slotConfig)
                    .dayType(DayType.ALL)
                    .startTime(LocalTime.of(5, 30))
                    .endTime(LocalTime.of(8, 0))
                    .basePrice(basePrice * 0.80) // 20% discount
                    .extraCharge(0.0)
                    .reason("Early bird discount")
                    .priority(4)
                    .enabled(true)
                    .build();
            resourcePriceRuleRepository.save(earlyMorningRule);

            log.info("✅ Created 4 price rules for {}", resource.getName());
        }
    }

    private void initializeActivities() {
        log.info("Initializing activities...");
        List<Activity> activities = new ArrayList<>();
        activities.add(Activity.builder().code("FOOTBALL").name("Football").enabled(true).build());
        activities.add(Activity.builder().code("CRICKET").name("Cricket").enabled(true).build());
        activities.add(Activity.builder().code("BOWLING").name("Bowling").enabled(true).build());
        activities.add(Activity.builder().code("PADEL").name("Padel Ball").enabled(true).build());
        activities.add(Activity.builder().code("BADMINTON").name("Badminton").enabled(true).build());
        activities.add(Activity.builder().code("TENNIS").name("Tennis").enabled(true).build());
        activities.add(Activity.builder().code("SWIMMING").name("Swimming").enabled(true).build());
        activities.add(Activity.builder().code("BASKETBALL").name("Basketball").enabled(true).build());
        activities.add(Activity.builder().code("ARCADE").name("Arcade").enabled(true).build());
        activities.add(Activity.builder().code("GYM").name("Gym").enabled(true).build());
        activities.add(Activity.builder().code("SPA").name("Spa").enabled(true).build());
        activities.add(Activity.builder().code("STUDIO").name("Studio").enabled(true).build());
        activities.add(Activity.builder().code("CONFERENCE").name("Conference").enabled(true).build());
        activities.add(Activity.builder().code("PARTY_HALL").name("Party Hall").enabled(true).build());
        activityRepository.saveAll(activities);
        log.info("Activities initialized.");
    }


    /**
     * Initialize expense categories for a specific admin
     */
    private void createExpenseCategoriesForAdmin(AdminProfile adminProfile) {
        log.info("Initializing expense categories for admin {}...", adminProfile.getBusinessName());
        List<ExpenseCategory> categories = new ArrayList<>();

        // FIXED EXPENSES
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Electricity Bill")
                .type(ExpenseType.FIXED)
                .build());
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Water Bill")
                .type(ExpenseType.FIXED)
                .build());
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Rent")
                .type(ExpenseType.FIXED)
                .build());
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Staff Salary")
                .type(ExpenseType.FIXED)
                .build());
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Internet & Phone")
                .type(ExpenseType.FIXED)
                .build());
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Insurance")
                .type(ExpenseType.FIXED)
                .build());
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Subscription & Software")
                .type(ExpenseType.FIXED)
                .build());

        // VARIABLE EXPENSES
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Maintenance & Repairs")
                .type(ExpenseType.VARIABLE)
                .build());
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Equipment Purchase")
                .type(ExpenseType.VARIABLE)
                .build());
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Cleaning Supplies")
                .type(ExpenseType.VARIABLE)
                .build());
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Inventory Purchase")
                .type(ExpenseType.VARIABLE)
                .build());
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Marketing & Advertising")
                .type(ExpenseType.VARIABLE)
                .build());
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Transportation")
                .type(ExpenseType.VARIABLE)
                .build());
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Office Supplies")
                .type(ExpenseType.VARIABLE)
                .build());
        categories.add(ExpenseCategory.builder()
                .adminProfile(adminProfile)
                .name("Miscellaneous")
                .type(ExpenseType.VARIABLE)
                .build());

        expenseCategoryRepository.saveAll(categories);
        log.info("Expense categories initialized for admin {}.", adminProfile.getBusinessName());
    }

    /*
     * COMMENTED OUT: Full sample data initialization
     * Use initializeMinimalTestService() instead for development
     */

    /**
     * Initialize sample data: manager user and sample services
     */
    /*
    private void initializeSampleData() {
        log.info("Initializing sample data...");

        // Create manager user using UserRegistrationService
        User manager = userRegistrationService.registerNewUser("+919460629707");
        manager.setRole(Role.MANAGER);
        manager.setName("System Manager");
        manager.setEmail("manager@ezturf.com");
        manager.setEnabled(true);
        userRepository.save(manager);
        log.info("Created manager user: {}", manager.getPhone());

        // Create 10 sample admins
        initializeAdmins();

        // Create 20 sample services
        initializeServices();
    }
    */

    /**
     * Initialize 10 sample admins with profiles
     */
    /*
    private void initializeAdmins() {
        log.info("Initializing 10 sample admins...");

        String[][] adminData = {
            {"Rajesh Kumar", "+919876543210", "rajesh.kumar@ezturf.com", "Mumbai", "Play Arena Mumbai", "123 Andheri West, Mumbai", "27AABCU9603R1ZM"},
            {"Priya Sharma", "+919876543211", "priya.sharma@ezturf.com", "Delhi", "Delhi Sports Hub", "456 Connaught Place, Delhi", "07AACCP1234F1Z5"},
            {"Amit Patel", "+919876543212", "amit.patel@ezturf.com", "Bangalore", "Bangalore Turf Center", "789 Koramangala, Bangalore", "29AADCP5678G1ZP"},
            {"Sneha Reddy", "+919876543213", "sneha.reddy@ezturf.com", "Hyderabad", "Hyderabad Sports Arena", "321 Banjara Hills, Hyderabad", "36AAECP9012H1ZQ"},
            {"Vikram Singh", "+919876543214", "vikram.singh@ezturf.com", "Jaipur", "Jaipur Turf Palace", "654 Malviya Nagar, Jaipur", "08AAFCP3456I1ZR"},
            {"Ananya Iyer", "+919876543215", "ananya.iyer@ezturf.com", "Chennai", "Chennai Sports Complex", "987 Adyar, Chennai", "33AAGCP7890J1ZS"},
            {"Rahul Mehta", "+919876543216", "rahul.mehta@ezturf.com", "Pune", "Pune Play Zone", "147 Kothrud, Pune", "27AAHCP1234K1ZT"},
            {"Kavya Nair", "+919876543217", "kavya.nair@ezturf.com", "Kochi", "Kochi Turf Center", "258 Ernakulam, Kochi", "32AAICP5678L1ZU"},
            {"Arjun Gupta", "+919876543218", "arjun.gupta@ezturf.com", "Kolkata", "Kolkata Sports Hub", "369 Salt Lake, Kolkata", "19AAJCP9012M1ZV"},
            {"Meera Joshi", "+919876543219", "meera.joshi@ezturf.com", "Ahmedabad", "Ahmedabad Turf Arena", "741 Satellite, Ahmedabad", "24AAKCP3456N1ZW"}
        };

        for (String[] data : adminData) {
            // Create User using UserRegistrationService
            User adminUser = userRegistrationService.registerNewUser(data[1]);
            adminUser.setName(data[0]);
            adminUser.setEmail(data[2]);
            adminUser.setRole(Role.ADMIN);
            adminUser.setEnabled(true);
            adminUser = userRepository.save(adminUser);

            // Create AdminProfile
            AdminProfile adminProfile = AdminProfile.builder()
                    .user(adminUser)
                    .city(data[3])
                    .businessName(data[4])
                    .businessAddress(data[5])
                    .gstNumber(data[6])
                    .build();
            adminProfileRepository.save(adminProfile);

            log.info("Created admin: {} - {}", adminUser.getName(), adminUser.getPhone());
        }

        log.info("Successfully created 10 sample admins");
    }
    */

    /**
     * Initialize 100 sample services with location data and assign them randomly to admins
     */
    /*
    private void initializeServices() {
        log.info("Initializing 100 sample services with location data...");

        // Get all admin profiles
        List<AdminProfile> adminProfiles = adminProfileRepository.findAll();
        if (adminProfiles.isEmpty()) {
            log.warn("No admin profiles found. Skipping service initialization.");
            return;
        }


        // Format: {name, location, city, latitude, longitude, description, phone, price, primaryActivityCode, imageUrl1, imageUrl2, imageUrl3}
        String[][] serviceData = {
            // FOOTBALL Services - 15
            {"Premium Football Arena", "Andheri West, Mumbai", "Mumbai", "19.1334", "72.8267", "5v5 and 7v7 football turf with floodlights", "+919876501001", "1000.0", "FOOTBALL", "https://images.unsplash.com/photo-1529900748604-07564a03e7a6?w=800", "https://images.unsplash.com/photo-1575361204480-aadea25e6e68?w=800", "https://images.unsplash.com/photo-1518605348416-72580200d3f8?w=800"},
            {"Striker Football Zone", "Bandra West, Mumbai", "Mumbai", "19.0596", "72.8295", "Compact 5v5 football turf in prime location", "+919876501002", "800.0", "FOOTBALL", "https://images.unsplash.com/photo-1551958219-acbc608c6377?w=800", "https://images.unsplash.com/photo-1560272564-c83b66b1ad12?w=800", "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=800"},
            {"Elite Cricket Ground", "Connaught Place, Delhi", "Delhi", "28.6315", "77.2167", "Full-size cricket ground", "+919876501016", "1200.0", "CRICKET", "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=800", "https://images.unsplash.com/photo-1593341646782-e0b495cffd32?w=800", "https://images.unsplash.com/photo-1624526267942-ab0ff8a3e972?w=800"},
            {"Sports Hub Football Turf", "Koramangala, Bangalore", "Bengaluru", "12.9352", "77.6245", "International standard turf", "+919876501031", "900.0", "FOOTBALL", "https://images.unsplash.com/photo-1529900748604-07564a03e7a6?w=800", "https://images.unsplash.com/photo-1575361204480-aadea25e6e68?w=800", "https://images.unsplash.com/photo-1518605348416-72580200d3f8?w=800"},
            {"Victory Football Arena", "Banjara Hills, Hyderabad", "Hyderabad", "17.4126", "78.4439", "Premium football ground", "+919876501046", "950.0", "FOOTBALL", "https://images.unsplash.com/photo-1551958219-acbc608c6377?w=800", "https://images.unsplash.com/photo-1560272564-c83b66b1ad12?w=800", "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=800"},
            {"City Sports Complex", "Kothrud, Pune", "Pune", "18.5074", "73.8077", "Multi-sport football facility", "+919876501056", "850.0", "FOOTBALL", "https://images.unsplash.com/photo-1529900748604-07564a03e7a6?w=800", "https://images.unsplash.com/photo-1575361204480-aadea25e6e68?w=800", "https://images.unsplash.com/photo-1518605348416-72580200d3f8?w=800"},
            {"Champion Cricket Ground", "Adyar, Chennai", "Chennai", "13.0067", "80.2574", "Professional cricket ground", "+919876501066", "1000.0", "CRICKET", "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=800", "https://images.unsplash.com/photo-1593341646782-e0b495cffd32?w=800", "https://images.unsplash.com/photo-1624526267942-ab0ff8a3e972?w=800"},
            {"Royal Football Academy", "Malviya Nagar, Jaipur", "Jaipur", "26.8523", "75.8131", "Football ground with coaching", "+919876501076", "1100.0", "FOOTBALL", "https://images.unsplash.com/photo-1551958219-acbc608c6377?w=800", "https://images.unsplash.com/photo-1560272564-c83b66b1ad12?w=800", "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=800"},
            {"Beach Football Arena", "Ernakulam, Kochi", "Kochi", "9.9312", "76.2673", "Beach-style football turf", "+919876501078", "950.0", "FOOTBALL", "https://images.unsplash.com/photo-1529900748604-07564a03e7a6?w=800", "https://images.unsplash.com/photo-1575361204480-aadea25e6e68?w=800", "https://images.unsplash.com/photo-1518605348416-72580200d3f8?w=800"},
            {"Metro Football Court", "Salt Lake, Kolkata", "Kolkata", "22.5726", "88.3639", "Indoor football court", "+919876501080", "800.0", "FOOTBALL", "https://images.unsplash.com/photo-1551958219-acbc608c6377?w=800", "https://images.unsplash.com/photo-1560272564-c83b66b1ad12?w=800", "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=800"},

            // ARCADE Services - 10
            {"Game Zone Arcade", "Powai, Mumbai", "Mumbai", "19.1176", "72.9060", "50+ arcade games, VR zone, racing simulators", "+919876502001", "500.0", "ARCADE", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800", "https://images.unsplash.com/photo-1534423861386-85a16f5d13fd?w=800"},
            {"Fun City Gaming", "Saket, Delhi", "Delhi", "28.5244", "77.2066", "Family entertainment center with arcade games", "+919876502002", "450.0", "ARCADE", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800", "https://images.unsplash.com/photo-1534423861386-85a16f5d13fd?w=800"},
            {"Pixel Paradise Arcade", "Indiranagar, Bangalore", "Bengaluru", "12.9716", "77.6412", "Retro and modern arcade games", "+919876502003", "400.0", "ARCADE", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800", "https://images.unsplash.com/photo-1534423861386-85a16f5d13fd?w=800"},
            {"VR World Gaming", "HITEC City, Hyderabad", "Hyderabad", "17.4435", "78.3772", "Virtual reality gaming experience", "+919876502004", "600.0", "ARCADE", "https://images.unsplash.com/photo-1622979135225-d2ba269fb1ac?w=800", "https://images.unsplash.com/photo-1593508512255-86ab42a8e620?w=800", "https://images.unsplash.com/photo-1592478411213-6153e4ebc07d?w=800"},
            {"Super Games Arena", "Hinjewadi, Pune", "Pune", "18.5912", "73.7389", "Racing simulators and arcade classics", "+919876502005", "350.0", "ARCADE", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800", "https://images.unsplash.com/photo-1534423861386-85a16f5d13fd?w=800"},
            {"Infinity Arcade", "T Nagar, Chennai", "Chennai", "13.0418", "80.2341", "Multi-floor gaming arcade", "+919876502006", "400.0", "ARCADE", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800", "https://images.unsplash.com/photo-1534423861386-85a16f5d13fd?w=800"},
            {"Joystick Junction", "C-Scheme, Jaipur", "Jaipur", "26.9124", "75.7873", "Classic and modern arcade games", "+919876502007", "300.0", "ARCADE", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800", "https://images.unsplash.com/photo-1534423861386-85a16f5d13fd?w=800"},
            {"Gaming Galaxy", "Kakkanad, Kochi", "Kochi", "10.0104", "76.3499", "Premium gaming experience", "+919876502008", "450.0", "ARCADE", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800", "https://images.unsplash.com/photo-1534423861386-85a16f5d13fd?w=800"},
            {"Arcade Adventure", "New Town, Kolkata", "Kolkata", "22.5957", "88.4716", "Family friendly arcade center", "+919876502009", "350.0", "ARCADE", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800", "https://images.unsplash.com/photo-1534423861386-85a16f5d13fd?w=800"},
            {"Digital Den Arcade", "Satellite, Ahmedabad", "Ahmedabad", "23.0225", "72.5714", "Latest gaming technology", "+919876502010", "400.0", "ARCADE", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800", "https://images.unsplash.com/photo-1534423861386-85a16f5d13fd?w=800"},

            // POOL Services - 10
            {"Aqua Paradise Pool", "Juhu, Mumbai", "Mumbai", "19.1075", "72.8263", "Olympic size swimming pool with coaching", "+919876503001", "800.0", "SWIMMING", "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?w=800", "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800", "https://images.unsplash.com/photo-1560090995-01632a28895b?w=800"},
            {"Blue Lagoon Swimming", "Vasant Kunj, Delhi", "Delhi", "28.5177", "77.1590", "Temperature controlled indoor pool", "+919876503002", "700.0", "SWIMMING", "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?w=800", "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800", "https://images.unsplash.com/photo-1560090995-01632a28895b?w=800"},
            {"Splash Zone Pool Club", "HSR Layout, Bangalore", "Bengaluru", "12.9082", "77.6476", "Infinity pool with kids section", "+919876503003", "750.0", "SWIMMING", "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?w=800", "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800", "https://images.unsplash.com/photo-1560090995-01632a28895b?w=800"},
            {"AquaFit Swimming Center", "Jubilee Hills, Hyderabad", "Hyderabad", "17.4239", "78.4128", "Professional swimming training", "+919876503004", "650.0", "SWIMMING", "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?w=800", "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800", "https://images.unsplash.com/photo-1560090995-01632a28895b?w=800"},
            {"Wave Pool Resort", "Baner, Pune", "Pune", "18.5591", "73.7866", "Wave pool and water slides", "+919876503005", "900.0", "SWIMMING", "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?w=800", "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800", "https://images.unsplash.com/photo-1560090995-01632a28895b?w=800"},
            {"Ocean Blue Pool", "Velachery, Chennai", "Chennai", "12.9750", "80.2210", "Semi-Olympic swimming pool", "+919876503006", "600.0", "SWIMMING", "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?w=800", "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800", "https://images.unsplash.com/photo-1560090995-01632a28895b?w=800"},
            {"Royal Swimmers Club", "Vaishali Nagar, Jaipur", "Jaipur", "26.9110", "75.7350", "Luxury pool with spa", "+919876503007", "850.0", "SWIMMING", "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?w=800", "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800", "https://images.unsplash.com/photo-1560090995-01632a28895b?w=800"},
            {"Tropical Pool Paradise", "Marine Drive, Kochi", "Kochi", "9.9816", "76.2756", "Rooftop infinity pool", "+919876503008", "700.0", "SWIMMING", "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?w=800", "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800", "https://images.unsplash.com/photo-1560090995-01632a28895b?w=800"},
            {"Deep Blue Swimming", "Ballygunge, Kolkata", "Kolkata", "22.5308", "88.3639", "Indoor heated pool", "+919876503009", "650.0", "SWIMMING", "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?w=800", "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800", "https://images.unsplash.com/photo-1560090995-01632a28895b?w=800"},
            {"Aquatic Center", "Prahlad Nagar, Ahmedabad", "Ahmedabad", "23.0073", "72.5170", "Multi-lane competition pool", "+919876503010", "750.0", "SWIMMING", "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?w=800", "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800", "https://images.unsplash.com/photo-1560090995-01632a28895b?w=800"},

            // BOWLING Services - 10
            {"Strike Zone Bowling", "Malad West, Mumbai", "Mumbai", "19.1864", "72.8485", "12-lane bowling alley with lounge", "+919876504001", "600.0", "BOWLING", "https://images.unsplash.com/photo-1538510624418-6221e2ebd12d?w=800", "https://images.unsplash.com/photo-1542810634-71277d95dcbb?w=800", "https://images.unsplash.com/photo-1516199423456-1f1e91b06953?w=800"},
            {"Pin Masters Bowling", "Rajouri Garden, Delhi", "Delhi", "28.6414", "77.1202", "Premium bowling experience", "+919876504002", "550.0", "BOWLING", "https://images.unsplash.com/photo-1538510624418-6221e2ebd12d?w=800", "https://images.unsplash.com/photo-1542810634-71277d95dcbb?w=800", "https://images.unsplash.com/photo-1516199423456-1f1e91b06953?w=800"},
            {"Kingpin Bowling Alley", "Whitefield, Bangalore", "Bengaluru", "12.9698", "77.7499", "Cosmic bowling nights", "+919876504003", "500.0", "BOWLING", "https://images.unsplash.com/photo-1538510624418-6221e2ebd12d?w=800", "https://images.unsplash.com/photo-1542810634-71277d95dcbb?w=800", "https://images.unsplash.com/photo-1516199423456-1f1e91b06953?w=800"},
            {"Strike & Spare Lanes", "Madhapur, Hyderabad", "Hyderabad", "17.4483", "78.3915", "Family bowling center", "+919876504004", "450.0", "BOWLING", "https://images.unsplash.com/photo-1538510624418-6221e2ebd12d?w=800", "https://images.unsplash.com/photo-1542810634-71277d95dcbb?w=800", "https://images.unsplash.com/photo-1516199423456-1f1e91b06953?w=800"},
            {"Roll & Rock Bowling", "Magarpatta, Pune", "Pune", "18.5157", "73.9293", "8-lane bowling with cafe", "+919876504005", "400.0", "BOWLING", "https://images.unsplash.com/photo-1538510624418-6221e2ebd12d?w=800", "https://images.unsplash.com/photo-1542810634-71277d95dcbb?w=800", "https://images.unsplash.com/photo-1516199423456-1f1e91b06953?w=800"},
            {"Perfect Strike Bowling", "Anna Nagar, Chennai", "Chennai", "13.0850", "80.2101", "Professional bowling lanes", "+919876504006", "500.0", "BOWLING", "https://images.unsplash.com/photo-1538510624418-6221e2ebd12d?w=800", "https://images.unsplash.com/photo-1542810634-71277d95dcbb?w=800", "https://images.unsplash.com/photo-1516199423456-1f1e91b06953?w=800"},
            {"Bowl-O-Rama Jaipur", "Tonk Road, Jaipur", "Jaipur", "26.8600", "75.8000", "Glow bowling experience", "+919876504007", "450.0", "BOWLING", "https://images.unsplash.com/photo-1538510624418-6221e2ebd12d?w=800", "https://images.unsplash.com/photo-1542810634-71277d95dcbb?w=800", "https://images.unsplash.com/photo-1516199423456-1f1e91b06953?w=800"},
            {"Alley Cats Bowling", "MG Road, Kochi", "Kochi", "9.9687", "76.2893", "Modern bowling center", "+919876504008", "400.0", "BOWLING", "https://images.unsplash.com/photo-1538510624418-6221e2ebd12d?w=800", "https://images.unsplash.com/photo-1542810634-71277d95dcbb?w=800", "https://images.unsplash.com/photo-1516199423456-1f1e91b06953?w=800"},
            {"Strike Force Lanes", "Park Street, Kolkata", "Kolkata", "22.5534", "88.3540", "Premium bowling lounge", "+919876504009", "550.0", "BOWLING", "https://images.unsplash.com/photo-1538510624418-6221e2ebd12d?w=800", "https://images.unsplash.com/photo-1542810634-71277d95dcbb?w=800", "https://images.unsplash.com/photo-1516199423456-1f1e91b06953?w=800"},
            {"Pin Point Bowling", "CG Road, Ahmedabad", "Ahmedabad", "23.0258", "72.5626", "Hi-tech bowling alley", "+919876504010", "500.0", "BOWLING", "https://images.unsplash.com/photo-1538510624418-6221e2ebd12d?w=800", "https://images.unsplash.com/photo-1542810634-71277d95dcbb?w=800", "https://images.unsplash.com/photo-1516199423456-1f1e91b06953?w=800"},

            // BADMINTON Services - 10
            {"Shuttle Star Courts", "Chembur, Mumbai", "Mumbai", "19.0633", "72.8997", "6 indoor badminton courts", "+919876505001", "400.0", "BADMINTON", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800", "https://images.unsplash.com/photo-1613918108466-292b78a8ef95?w=800", "https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800"},
            {"Ace Badminton Arena", "Pitampura, Delhi", "Delhi", "28.6960", "77.1320", "Professional badminton training", "+919876505002", "350.0", "BADMINTON", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800", "https://images.unsplash.com/photo-1613918108466-292b78a8ef95?w=800", "https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800"},
            {"Smash Point Badminton", "Jayanagar, Bangalore", "Bengaluru", "12.9250", "77.5937", "Olympic standard courts", "+919876505003", "450.0", "BADMINTON", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800", "https://images.unsplash.com/photo-1613918108466-292b78a8ef95?w=800", "https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800"},
            {"Shuttle Express Courts", "Kukatpally, Hyderabad", "Hyderabad", "17.4948", "78.4142", "Air-conditioned courts", "+919876505004", "400.0", "BADMINTON", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800", "https://images.unsplash.com/photo-1613918108466-292b78a8ef95?w=800", "https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800"},
            {"Feather Touch Badminton", "Wakad, Pune", "Pune", "18.5978", "73.7644", "4 synthetic courts", "+919876505005", "350.0", "BADMINTON", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800", "https://images.unsplash.com/photo-1613918108466-292b78a8ef95?w=800", "https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800"},
            {"Net Masters Badminton", "Guindy, Chennai", "Chennai", "13.0097", "80.2209", "Professional coaching center", "+919876505006", "400.0", "BADMINTON", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800", "https://images.unsplash.com/photo-1613918108466-292b78a8ef95?w=800", "https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800"},
            {"Swift Shuttle Academy", "Raja Park, Jaipur", "Jaipur", "26.9000", "75.8100", "Training and play courts", "+919876505007", "300.0", "BADMINTON", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800", "https://images.unsplash.com/photo-1613918108466-292b78a8ef95?w=800", "https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800"},
            {"Drop Shot Courts", "Edappally, Kochi", "Kochi", "10.0261", "76.3125", "Premium badminton facility", "+919876505008", "350.0", "BADMINTON", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800", "https://images.unsplash.com/photo-1613918108466-292b78a8ef95?w=800", "https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800"},
            {"Rally Point Badminton", "Gariahat, Kolkata", "Kolkata", "22.5186", "88.3656", "Indoor synthetic courts", "+919876505009", "400.0", "BADMINTON", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800", "https://images.unsplash.com/photo-1613918108466-292b78a8ef95?w=800", "https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800"},
            {"Clear Shot Academy", "Bodakdev, Ahmedabad", "Ahmedabad", "23.0513", "72.5170", "6 badminton courts", "+919876505010", "350.0", "BADMINTON", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800", "https://images.unsplash.com/photo-1613918108466-292b78a8ef95?w=800", "https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800"},

            // TENNIS Services - 8
            {"Grand Slam Tennis Club", "Goregaon East, Mumbai", "Mumbai", "19.1653", "72.8526", "4 clay and hard courts", "+919876506001", "800.0", "TENNIS", "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800", "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=800", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800"},
            {"Ace Tennis Academy", "Greater Kailash, Delhi", "Delhi", "28.5494", "77.2426", "Professional tennis training", "+919876506002", "900.0", "TENNIS", "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800", "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=800", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800"},
            {"Centre Court Tennis", "Bellandur, Bangalore", "Bengaluru", "12.9266", "77.6789", "6 all-weather courts", "+919876506003", "750.0", "TENNIS", "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800", "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=800", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800"},
            {"Match Point Tennis", "Gachibowli, Hyderabad", "Hyderabad", "17.4400", "78.3489", "International standard courts", "+919876506004", "700.0", "TENNIS", "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800", "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=800", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800"},
            {"Baseline Tennis Club", "Kalyani Nagar, Pune", "Pune", "18.5486", "73.9048", "4 synthetic courts", "+919876506005", "650.0", "TENNIS", "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800", "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=800", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800"},
            {"Love All Tennis", "OMR, Chennai", "Chennai", "12.8700", "80.2206", "Professional coaching center", "+919876506006", "700.0", "TENNIS", "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800", "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=800", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800"},
            {"Deuce Tennis Academy", "Mansarovar, Jaipur", "Jaipur", "26.8800", "75.7600", "Clay court facility", "+919876506007", "600.0", "TENNIS", "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800", "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=800", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800"},
            {"TopSpin Tennis Club", "Panampilly Nagar, Kochi", "Kochi", "9.9520", "76.2920", "4 premium courts", "+919876506008", "650.0", "TENNIS", "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800", "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=800", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800"},

            // BASKETBALL Services - 7
            {"Dunk City Basketball", "Dahisar East, Mumbai", "Mumbai", "19.2544", "72.8643", "Full court with coaching", "+919876507001", "500.0", "BASKETBALL", "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800", "https://images.unsplash.com/photo-1519861531473-92002639313?w=800", "https://images.unsplash.com/photo-1504450758481-7338eba7524a?w=800"},
            {"Hoops Heaven Arena", "Janakpuri, Delhi", "Delhi", "28.6219", "77.0854", "Indoor basketball court", "+919876507002", "550.0", "BASKETBALL", "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800", "https://images.unsplash.com/photo-1519861531473-92002639313?w=800", "https://images.unsplash.com/photo-1504450758481-7338eba7524a?w=800"},
            {"Slam Dunk Academy", "BTM Layout, Bangalore", "Bengaluru", "12.9165", "77.6101", "Professional training", "+919876507003", "450.0", "BASKETBALL", "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800", "https://images.unsplash.com/photo-1519861531473-92002639313?w=800", "https://images.unsplash.com/photo-1504450758481-7338eba7524a?w=800"},
            {"Court Kings Basketball", "Kondapur, Hyderabad", "Hyderabad", "17.4614", "78.3641", "3x3 and full court", "+919876507004", "400.0", "BASKETBALL", "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800", "https://images.unsplash.com/photo-1519861531473-92002639313?w=800", "https://images.unsplash.com/photo-1504450758481-7338eba7524a?w=800"},
            {"Net Swish Courts", "Aundh, Pune", "Pune", "18.5590", "73.8077", "Outdoor basketball courts", "+919876507005", "350.0", "BASKETBALL", "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800", "https://images.unsplash.com/photo-1519861531473-92002639313?w=800", "https://images.unsplash.com/photo-1504450758481-7338eba7524a?w=800"},
            {"Basket Pro Arena", "Mylapore, Chennai", "Chennai", "13.0339", "80.2619", "Air-conditioned indoor court", "+919876507006", "500.0", "BASKETBALL", "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800", "https://images.unsplash.com/photo-1519861531473-92002639313?w=800", "https://images.unsplash.com/photo-1504450758481-7338eba7524a?w=800"},
            {"Triple Threat Courts", "Sodala, Jaipur", "Jaipur", "26.9200", "75.7800", "Training and recreation", "+919876507007", "400.0", "BASKETBALL", "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800", "https://images.unsplash.com/photo-1519861531473-92002639313?w=800", "https://images.unsplash.com/photo-1504450758481-7338eba7524a?w=800"},

            // GYM Services - 10
            {"Iron Paradise Gym", "Mulund West, Mumbai", "Mumbai", "19.1722", "72.9565", "Premium fitness equipment", "+919876508001", "300.0", "GYM", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800", "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800"},
            {"Flex Fitness Center", "Lajpat Nagar, Delhi", "Delhi", "28.5677", "77.2431", "Full gym with trainers", "+919876508002", "350.0", "GYM", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800", "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800"},
            {"Power House Gym", "Marathahalli, Bangalore", "Bengaluru", "12.9591", "77.6974", "CrossFit and weight training", "+919876508003", "400.0", "GYM", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800", "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800"},
            {"FitZone Gym", "Secunderabad, Hyderabad", "Hyderabad", "17.4399", "78.4983", "Modern fitness center", "+919876508004", "300.0", "GYM", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800", "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800"},
            {"Muscle Factory", "Pimpri, Pune", "Pune", "18.6298", "73.8057", "Bodybuilding focused gym", "+919876508005", "250.0", "GYM", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800", "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800"},
            {"Core Fitness Studio", "Porur, Chennai", "Chennai", "13.0358", "80.1572", "Functional training", "+919876508006", "350.0", "GYM", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800", "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800"},
            {"Beast Mode Gym", "Jawahar Nagar, Jaipur", "Jaipur", "26.9300", "75.8200", "Heavy equipment gym", "+919876508007", "280.0", "GYM", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800", "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800"},
            {"Workout World", "Vytilla, Kochi", "Kochi", "9.9700", "76.3100", "Complete fitness solution", "+919876508008", "300.0", "GYM", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800", "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800"},
            {"Strength Studio", "Behala, Kolkata", "Kolkata", "22.4990", "88.3040", "Personal training focus", "+919876508009", "350.0", "GYM", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800", "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800"},
            {"Peak Fitness Center", "Navrangpura, Ahmedabad", "Ahmedabad", "23.0350", "72.5600", "Premium gym facility", "+919876508010", "320.0", "GYM", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800", "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800"},

            // SPA Services - 5
            {"Serenity Spa & Wellness", "Kurla West, Mumbai", "Mumbai", "19.0728", "72.8826", "Luxury spa treatments", "+919876509001", "1500.0", "SPA", "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=800", "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?w=800", "https://images.unsplash.com/photo-1515377905703-c4788e51af15?w=800"},
            {"Tranquil Touch Spa", "Mayur Vihar, Delhi", "Delhi", "28.6082", "77.2986", "Ayurvedic therapies", "+919876509002", "1200.0", "SPA", "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=800", "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?w=800", "https://images.unsplash.com/photo-1515377905703-c4788e51af15?w=800"},
            {"Zen Wellness Spa", "Electronic City, Bangalore", "Bengaluru", "12.8456", "77.6603", "Relaxation therapies", "+919876509003", "1000.0", "SPA", "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=800", "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?w=800", "https://images.unsplash.com/photo-1515377905703-c4788e51af15?w=800"},
            {"Bliss Spa Retreat", "LB Nagar, Hyderabad", "Hyderabad", "17.3523", "78.5527", "Thai massage specialty", "+919876509004", "1100.0", "SPA", "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=800", "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?w=800", "https://images.unsplash.com/photo-1515377905703-c4788e51af15?w=800"},
            {"Harmony Spa Center", "Shivaji Nagar, Pune", "Pune", "18.5304", "73.8567", "Wellness treatments", "+919876509005", "900.0", "SPA", "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=800", "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?w=800", "https://images.unsplash.com/photo-1515377905703-c4788e51af15?w=800"},

            // STUDIO Services - 5
            {"Creative Lens Studio", "Ghatkopar East, Mumbai", "Mumbai", "19.0860", "72.9081", "Photography and video studio", "+919876510001", "2000.0", "STUDIO", "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=800", "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800", "https://images.unsplash.com/photo-1520583457224-aee1134532b4?w=800"},
            {"SoundWave Recording", "Karol Bagh, Delhi", "Delhi", "28.6514", "77.1906", "Professional recording studio", "+919876510002", "2500.0", "STUDIO", "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=800", "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800", "https://images.unsplash.com/photo-1520583457224-aee1134532b4?w=800"},
            {"Frame Perfect Studio", "Malleshwaram, Bangalore", "Bengaluru", "13.0006", "77.5707", "Photo and video production", "+919876510003", "1800.0", "STUDIO", "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=800", "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800", "https://images.unsplash.com/photo-1520583457224-aee1134532b4?w=800"},
            {"Beat Box Recording", "Ameerpet, Hyderabad", "Hyderabad", "17.4374", "78.4482", "Music production studio", "+919876510004", "2200.0", "STUDIO", "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=800", "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800", "https://images.unsplash.com/photo-1520583457224-aee1134532b4?w=800"},
            {"Click Studio Photography", "Tambaram, Chennai", "Chennai", "12.9249", "80.1000", "Portrait and event studio", "+919876510005", "1500.0", "STUDIO", "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=800", "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800", "https://images.unsplash.com/photo-1520583457224-aee1134532b4?w=800"},

            // CONFERENCE Services - 5
            {"Summit Conference Hall", "Borivali West, Mumbai", "Mumbai", "19.2403", "72.8565", "100-seater conference room", "+919876511001", "5000.0", "CONFERENCE", "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?w=800", "https://images.unsplash.com/photo-1431540015161-0bf868a2d407?w=800", "https://images.unsplash.com/photo-1505373877841-8d25f7d46678?w=800"},
            {"Corporate Meet Center", "Preet Vihar, Delhi", "Delhi", "28.6424", "77.2978", "Business meeting rooms", "+919876511002", "4000.0", "CONFERENCE", "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?w=800", "https://images.unsplash.com/photo-1431540015161-0bf868a2d407?w=800", "https://images.unsplash.com/photo-1505373877841-8d25f7d46678?w=800"},
            {"Synergy Conference Hub", "Yelahanka, Bangalore", "Bengaluru", "13.1007", "77.5963", "Modern conference facility", "+919876511003", "4500.0", "CONFERENCE", "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?w=800", "https://images.unsplash.com/photo-1431540015161-0bf868a2d407?w=800", "https://images.unsplash.com/photo-1505373877841-8d25f7d46678?w=800"},
            {"Business Bay Conference", "Banjara Hills, Hyderabad", "Hyderabad", "17.4200", "78.4500", "Premium meeting rooms", "+919876511004", "3500.0", "CONFERENCE", "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?w=800", "https://images.unsplash.com/photo-1431540015161-0bf868a2d407?w=800", "https://images.unsplash.com/photo-1505373877841-8d25f7d46678?w=800"},
            {"Professional Meet Hall", "Shivaji Nagar, Pune", "Pune", "18.5300", "73.8500", "Corporate event space", "+919876511005", "3000.0", "CONFERENCE", "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?w=800", "https://images.unsplash.com/photo-1431540015161-0bf868a2d407?w=800", "https://images.unsplash.com/photo-1505373877841-8d25f7d46678?w=800"},

            // PARTY_HALL Services - 5
            {"Grand Celebration Hall", "Kandivali West, Mumbai", "Mumbai", "19.2072", "72.8310", "500-capacity party venue", "+919876512001", "25000.0", "PARTY_HALL", "https://images.unsplash.com/photo-1519167758481-83f550bb49b3?w=800", "https://images.unsplash.com/photo-1469334031218-e382a71b716b?w=800", "https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=800"},
            {"Festive Banquet Hall", "Model Town, Delhi", "Delhi", "28.7154", "77.1946", "Wedding and party venue", "+919876512002", "30000.0", "PARTY_HALL", "https://images.unsplash.com/photo-1519167758481-83f550bb49b3?w=800", "https://images.unsplash.com/photo-1469334031218-e382a71b716b?w=800", "https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=800"},
            {"Royal Events Palace", "Banashankari, Bangalore", "Bengaluru", "12.9250", "77.5482", "Luxury party hall", "+919876512003", "20000.0", "PARTY_HALL", "https://images.unsplash.com/photo-1519167758481-83f550bb49b3?w=800", "https://images.unsplash.com/photo-1469334031218-e382a71b716b?w=800", "https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=800"},
            {"Celebration Station", "Ameerpet, Hyderabad", "Hyderabad", "17.4350", "78.4450", "Multi-purpose event hall", "+919876512004", "18000.0", "PARTY_HALL", "https://images.unsplash.com/photo-1519167758481-83f550bb49b3?w=800", "https://images.unsplash.com/photo-1469334031218-e382a71b716b?w=800", "https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=800"},
            {"Gala Grand Hall", "Aundh, Pune", "Pune", "18.5600", "73.8100", "Premium party venue", "+919876512005", "22000.0", "PARTY_HALL", "https://images.unsplash.com/photo-1519167758481-83f550bb49b3?w=800", "https://images.unsplash.com/photo-1469334031218-e382a71b716b?w=800", "https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=800"},

            // ==================== UDAIPUR SERVICES ====================
            // Udaipur, Rajasthan - The City of Lakes
            // Coordinates: 24.5854° N, 73.7125° E

            // FOOTBALL Services in Udaipur - 3
            {"Lake City Football Arena", "Sajjangarh Road, Udaipur", "Udaipur", "24.5900", "73.7200", "Premium 5v5 and 7v7 football turf with night floodlights", "+919876513001", "850.0", "FOOTBALL", "https://images.unsplash.com/photo-1529900748604-07564a03e7a6?w=800", "https://images.unsplash.com/photo-1575361204480-aadea25e6e68?w=800", "https://images.unsplash.com/photo-1518605348416-72580200d3f8?w=800"},
            {"Royal City Sports Ground", "Fateh Sagar Road, Udaipur", "Udaipur", "24.5820", "73.7150", "Full-size football ground with synthetic turf", "+919876513002", "900.0", "FOOTBALL", "https://images.unsplash.com/photo-1551958219-acbc608c6377?w=800", "https://images.unsplash.com/photo-1560272564-c83b66b1ad12?w=800", "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=800"},
            {"Pichola Sports Turf", "Lake Pichola, Udaipur", "Udaipur", "24.5750", "73.7180", "Scenic football ground near lake with modern facilities", "+919876513003", "950.0", "FOOTBALL", "https://images.unsplash.com/photo-1529900748604-07564a03e7a6?w=800", "https://images.unsplash.com/photo-1575361204480-aadea25e6e68?w=800", "https://images.unsplash.com/photo-1518605348416-72580200d3f8?w=800"},

            // CRICKET Services in Udaipur - 2
            {"Maharaja Cricket Ground", "Shobhagpura, Udaipur", "Udaipur", "24.5850", "73.7250", "International standard cricket pitch with practice grounds", "+919876513004", "1100.0", "CRICKET", "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=800", "https://images.unsplash.com/photo-1593341646782-e0b495cffd32?w=800", "https://images.unsplash.com/photo-1624526267942-ab0ff8a3e972?w=800"},
            {"Valley Cricket Academy", "Mandi Road, Udaipur", "Udaipur", "24.5780", "73.7100", "Professional cricket coaching center with multiple grounds", "+919876513005", "1050.0", "CRICKET", "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=800", "https://images.unsplash.com/photo-1593341646782-e0b495cffd32?w=800", "https://images.unsplash.com/photo-1624526267942-ab0ff8a3e972?w=800"},

            // BADMINTON Services in Udaipur - 2
            {"Palace Badminton Courts", "City Palace Area, Udaipur", "Udaipur", "24.5810", "73.7190", "4 indoor air-conditioned badminton courts", "+919876513006", "380.0", "BADMINTON", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800", "https://images.unsplash.com/photo-1613918108466-292b78a8ef95?w=800", "https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800"},
            {"Sunset Badminton Arena", "Didi Bari Road, Udaipur", "Udaipur", "24.5750", "73.7100", "Professional badminton facility with training programs", "+919876513007", "400.0", "BADMINTON", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800", "https://images.unsplash.com/photo-1613918108466-292b78a8ef95?w=800", "https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800"},

            // SWIMMING Services in Udaipur - 2
            {"Lake Crystal Swimming Pool", "Sajjangarh Waterfalls Area, Udaipur", "Udaipur", "24.5950", "73.7300", "Olympic-size swimming pool with kids section", "+919876513008", "750.0", "SWIMMING", "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?w=800", "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800", "https://images.unsplash.com/photo-1560090995-01632a28895b?w=800"},
            {"Rajasthani Heritage Pool", "Hiran Magri, Udaipur", "Udaipur", "24.5650", "73.7050", "Traditional style swimming complex with modern amenities", "+919876513009", "700.0", "SWIMMING", "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?w=800", "https://images.unsplash.com/photo-1519315901367-f34ff9154487?w=800", "https://images.unsplash.com/photo-1560090995-01632a28895b?w=800"},

            // ARCADE Services in Udaipur - 1
            {"Rajwada Gaming Zone", "Pannalal Road, Udaipur", "Udaipur", "24.5900", "73.7100", "40+ arcade games with VR gaming and racing simulators", "+919876513010", "500.0", "ARCADE", "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800", "https://images.unsplash.com/photo-1534423861386-85a16f5d13fd?w=800"}
        };

        // Track services per admin for even distribution
        int[] servicesPerAdmin = new int[adminProfiles.size()];

        int adminIndex = 0;
        for (int i = 0; i < serviceData.length; i++) {
            String[] data = serviceData[i];

            // Assign service to admin in round-robin fashion for even distribution
            AdminProfile assignedAdmin = adminProfiles.get(adminIndex % adminProfiles.size());

            // Parse primary activity code
            String activityCode = data[8];

            // Create image list
            List<String> images = new ArrayList<>();
            if (data.length > 9) {
                for (int j = 9; j < data.length; j++) {
                    images.add(data[j]);
                }
            }

            // Create Service with location data
            List<String> amenities = getAmenitiesForActivity(activityCode);

            Service service = Service.builder()
                    .name(data[0])
                    .location(data[1])
                    .city(data[2])
                    .latitude(Double.parseDouble(data[3]))
                    .longitude(Double.parseDouble(data[4]))
                    .description(data[5])
                    .contactNumber(data[6])
                    .availability(true)
                    .createdBy(assignedAdmin)
                    .images(images)
                    .amenities(amenities)
                    .build();
            service = serviceRepository.save(service);

            // Assign activities to service
            assignActivitiesToService(service, activityCode);

            // Create resources for this service based on activity code
            // (slots are now generated dynamically from ResourceSlotConfig)
            createResourcesForService(service, activityCode);

            // Track assignment
            servicesPerAdmin[adminIndex % adminProfiles.size()]++;

            log.info("Created service {}/{}: {} in {} - Assigned to: {}",
                    (i + 1),
                    serviceData.length,
                    service.getName(),
                    service.getCity(),
                    assignedAdmin.getUser().getName());

            adminIndex++;
        }

        // Log distribution summary
        log.info("Successfully created {} sample services across multiple cities", serviceData.length);
        log.info("Service distribution per admin:");
        for (int i = 0; i < adminProfiles.size(); i++) {
            log.info("  {} - {} services",
                    adminProfiles.get(i).getUser().getName(),
                    servicesPerAdmin[i]);
        }
    }


    /**
     * Create resources for a service based on its type
     * Each service type has different resource naming conventions
     */
    private void createResourcesForService(Service service, String activityCode) {
        List<ServiceResource> resources = new ArrayList<>();

        // Get base price from the service type
        Double basePrice = getBasePriceForActivity(activityCode);

        // Fetch common activities
        Activity primaryActivity = activityRepository.findByCode(activityCode).orElse(null);
        if (primaryActivity == null) return;

        List<Activity> primaryList = List.of(primaryActivity);

        switch (activityCode) {
            case "FOOTBALL":
            case "CRICKET":
                // Mixed capability demonstration
                Activity football = activityRepository.findByCode("FOOTBALL").orElse(null);
                Activity cricket = activityRepository.findByCode("CRICKET").orElse(null);

                List<Activity> both = new ArrayList<>();
                if (football != null) both.add(football);
                if (cricket != null) both.add(cricket);

                List<Activity> footballOnly = (football != null) ? List.of(football) : new ArrayList<>();
                List<Activity> cricketOnly = (cricket != null) ? List.of(cricket) : new ArrayList<>();

                // Football/Cricket turfs - Turf 1, Turf 2, etc.
                resources.add(createResource(service, "Turf 1", "Main turf (Multi-sport: Football + Cricket)", both));
                resources.add(createResource(service, "Turf 2", "Secondary turf (Football Only)", footballOnly));
                resources.add(createResource(service, "Turf 3", "Practice turf (Cricket Only)", cricketOnly));
                break;

            case "ARCADE":
                // Arcade games - different game zones
                resources.add(createResource(service, "VR Zone", "Virtual reality gaming area", primaryList));
                resources.add(createResource(service, "Racing Zone", "Racing simulators and games", primaryList));
                resources.add(createResource(service, "Classic Arcade", "Retro arcade machines", primaryList));
                resources.add(createResource(service, "Prize Games", "Ticket redemption games", primaryList));
                break;

            case "SWIMMING":
                // Swimming pools - different pool types
                resources.add(createResource(service, "Main Pool", "Olympic size swimming pool", primaryList));
                resources.add(createResource(service, "Kids Pool", "Shallow pool for children", primaryList));
                resources.add(createResource(service, "Lap Pool", "Lanes for lap swimming", primaryList));
                break;

            case "BOWLING":
                // Bowling alleys - individual lanes
                resources.add(createResource(service, "Lane 1", "Standard bowling lane", primaryList));
                resources.add(createResource(service, "Lane 2", "Standard bowling lane", primaryList));
                resources.add(createResource(service, "Lane 3", "Standard bowling lane", primaryList));
                resources.add(createResource(service, "Lane 4", "Standard bowling lane", primaryList));
                resources.add(createResource(service, "VIP Lane 1", "Premium lane with lounge", primaryList));
                resources.add(createResource(service, "VIP Lane 2", "Premium lane with lounge", primaryList));
                break;

            case "BADMINTON":
                // Badminton courts
                resources.add(createResource(service, "Court A", "Indoor synthetic court", primaryList));
                resources.add(createResource(service, "Court B", "Indoor synthetic court", primaryList));
                resources.add(createResource(service, "Court C", "Indoor wooden court", primaryList));
                resources.add(createResource(service, "Court D", "Air-conditioned court", primaryList));
                break;

            case "TENNIS":
            case "PADEL":
                // Tennis courts - clay, hard, grass
                resources.add(createResource(service, "Clay Court 1", "Red clay surface", primaryList));
                resources.add(createResource(service, "Clay Court 2", "Red clay surface", primaryList));
                resources.add(createResource(service, "Hard Court 1", "Acrylic hard court", primaryList));
                resources.add(createResource(service, "Hard Court 2", "Acrylic hard court", primaryList));
                break;

            case "BASKETBALL":
                // Basketball courts
                resources.add(createResource(service, "Full Court", "Full size basketball court", primaryList));
                resources.add(createResource(service, "Half Court A", "Half court for practice", primaryList));
                resources.add(createResource(service, "Half Court B", "Half court for 3x3", primaryList));
                break;

            case "GYM":
                // Gym zones/areas
                resources.add(createResource(service, "Weight Zone", "Free weights and machines", primaryList));
                resources.add(createResource(service, "Cardio Zone", "Treadmills and bikes", primaryList));
                resources.add(createResource(service, "CrossFit Area", "Functional training zone", primaryList));
                resources.add(createResource(service, "Personal Training Room", "Private training sessions", primaryList));
                break;

            case "SPA":
                // Spa treatment rooms
                resources.add(createResource(service, "Massage Room 1", "Swedish and deep tissue massage", primaryList));
                resources.add(createResource(service, "Massage Room 2", "Thai massage specialty", primaryList));
                resources.add(createResource(service, "Therapy Room", "Ayurvedic treatments", primaryList));
                resources.add(createResource(service, "Couples Suite", "Luxury couples treatment", primaryList));
                resources.add(createResource(service, "Steam & Sauna", "Steam room and sauna", primaryList));
                break;

            case "STUDIO":
                // Recording/Photography studios
                resources.add(createResource(service, "Studio A", "Main recording/photo studio", primaryList));
                resources.add(createResource(service, "Studio B", "Secondary studio", primaryList));
                resources.add(createResource(service, "Editing Room", "Post-production suite", primaryList));
                resources.add(createResource(service, "Green Screen Room", "Chroma key studio", primaryList));
                break;

            case "CONFERENCE":
                // Conference rooms
                resources.add(createResource(service, "Board Room", "10-seater executive room", primaryList));
                resources.add(createResource(service, "Conference Hall A", "50-seater meeting room", primaryList));
                resources.add(createResource(service, "Conference Hall B", "30-seater meeting room", primaryList));
                resources.add(createResource(service, "Training Room", "25-seater with projector", primaryList));
                break;

            case "PARTY_HALL":
                // Party/Event halls
                resources.add(createResource(service, "Grand Hall", "500+ capacity main hall", primaryList));
                resources.add(createResource(service, "Banquet Hall", "200 capacity banquet", primaryList));
                resources.add(createResource(service, "Terrace Area", "Open-air party space", primaryList));
                resources.add(createResource(service, "VIP Lounge", "Exclusive 50-seater lounge", primaryList));
                break;

            default:
                // Default - generic resources
                resources.add(createResource(service, "Resource 1", "Primary resource", primaryList));
                resources.add(createResource(service, "Resource 2", "Secondary resource", primaryList));
                break;
        }

        List<ServiceResource> savedResources = serviceResourceRepository.saveAll(resources);

        // Create slot configuration and generate slots for each resource
        for (ServiceResource resource : savedResources) {
            ResourceSlotConfig config = createSlotConfigAndSlots(resource, activityCode, basePrice);
            createPriceRulesForResource(config, activityCode);
        }

        log.debug("Created {} resources with slots for service: {}", savedResources.size(), service.getName());
    }

    /**
     * Get base price based on service type
     */
    private Double getBasePriceForActivity(String activityCode) {
        return switch (activityCode) {
            case "FOOTBALL", "CRICKET" -> 800.0;
            case "ARCADE" -> 200.0;
            case "SWIMMING" -> 300.0;
            case "BOWLING" -> 250.0;
            case "BADMINTON" -> 400.0;
            case "TENNIS", "PADEL" -> 600.0;
            case "BASKETBALL" -> 500.0;
            case "GYM" -> 150.0;
            case "SPA" -> 1500.0;
            case "STUDIO" -> 1000.0;
            case "CONFERENCE" -> 2000.0;
            case "PARTY_HALL" -> 5000.0;
            default -> 500.0;
        };
    }

    /**
     * Create slot configuration and generate slots for a resource
     */
    private ResourceSlotConfig createSlotConfigAndSlots(ServiceResource resource, String activityCode, Double basePrice) {
        // Get operating hours based on service type
        LocalTime openingTime = getOpeningTimeForActivity(activityCode);
        LocalTime closingTime = getClosingTimeForActivity(activityCode);
        int slotDuration = getSlotDurationForActivity(activityCode);
        double weekendMultiplier = getWeekendMultiplierForActivity(activityCode);

        // Create slot configuration
        ResourceSlotConfig config = ResourceSlotConfig.builder()
                .resource(resource)
                .openingTime(openingTime)
                .closingTime(closingTime)
                .slotDurationMinutes(slotDuration)
                .basePrice(basePrice)
                .enabled(true)
                .build();

        resourceSlotConfigRepository.save(config);

        // Slots are now generated dynamically from config, no need to pre-generate

        return config;
    }

    private LocalTime getOpeningTimeForActivity(String activityCode) {
        return switch (activityCode) {
            case "GYM" -> LocalTime.of(5, 0);  // 5 AM
            case "SWIMMING" -> LocalTime.of(6, 0);  // 6 AM
            case "FOOTBALL", "CRICKET", "BADMINTON", "TENNIS", "PADEL", "BASKETBALL" -> LocalTime.of(6, 0);  // 6 AM
            case "SPA" -> LocalTime.of(9, 0);  // 9 AM
            case "STUDIO", "CONFERENCE" -> LocalTime.of(8, 0);  // 8 AM
            case "ARCADE", "BOWLING" -> LocalTime.of(10, 0);  // 10 AM
            case "PARTY_HALL" -> LocalTime.of(10, 0);  // 10 AM
            default -> LocalTime.of(8, 0);
        };
    }

    private LocalTime getClosingTimeForActivity(String activityCode) {
        return switch (activityCode) {
            case "GYM" -> LocalTime.of(23, 0);  // 11 PM
            case "SWIMMING" -> LocalTime.of(21, 0);  // 9 PM
            case "FOOTBALL", "CRICKET" -> LocalTime.of(23, 0);  // 11 PM
            case "BADMINTON", "TENNIS", "PADEL", "BASKETBALL" -> LocalTime.of(22, 0);  // 10 PM
            case "SPA" -> LocalTime.of(21, 0);  // 9 PM
            case "STUDIO", "CONFERENCE" -> LocalTime.of(20, 0);  // 8 PM
            case "ARCADE", "BOWLING" -> LocalTime.of(23, 0);  // 11 PM
            case "PARTY_HALL" -> LocalTime.of(22, 0);  // 10 PM (allows 4 slots of 3 hours from 10 AM)
            default -> LocalTime.of(22, 0);
        };
    }

    private int getSlotDurationForActivity(String activityCode) {
        return switch (activityCode) {
            case "FOOTBALL", "CRICKET", "BASKETBALL" -> 60;  // 1 hour
            case "BADMINTON", "TENNIS", "PADEL" -> 60;  // 1 hour
            case "SWIMMING", "GYM" -> 60;  // 1 hour
            case "BOWLING" -> 30;  // 30 minutes per game
            case "ARCADE" -> 30;  // 30 minutes
            case "SPA" -> 60;  // 1 hour treatments
            case "STUDIO" -> 120;  // 2 hours
            case "CONFERENCE" -> 60;  // 1 hour
            case "PARTY_HALL" -> 180;  // 3 hours minimum
            default -> 60;
        };
    }

    private double getWeekendMultiplierForActivity(String activityCode) {
        return switch (activityCode) {
            case "FOOTBALL", "CRICKET", "BADMINTON", "TENNIS", "PADEL", "BASKETBALL" -> 1.25;  // 25% extra
            case "SWIMMING" -> 1.2;  // 20% extra
            case "BOWLING", "ARCADE" -> 1.3;  // 30% extra
            case "SPA" -> 1.15;  // 15% extra
            case "PARTY_HALL" -> 1.5;  // 50% extra on weekends
            default -> 1.2;
        };
    }

    // Note: generateSlotsForResource method removed - slots are now generated dynamically
    // from ResourceSlotConfig using SlotGeneratorService


    /**
     * Create price rules for a resource based on service type
     */
    private void createPriceRulesForResource(ResourceSlotConfig config, String activityCode) {
        List<ResourcePriceRule> rules = new ArrayList<>();

        if (activityCode.equals("FOOTBALL") || activityCode.equals("CRICKET")) {
            // Night lighting charge for Turfs (6 PM - 11 PM)
            rules.add(ResourcePriceRule.builder()
                    .resourceSlotConfig(config)
                    .dayType(DayType.ALL)
                    .startTime(LocalTime.of(18, 0))
                    .endTime(LocalTime.of(23, 0))
                    .extraCharge(200.0)
                    .reason("Night Lighting Charge")
                    .priority(1)
                    .enabled(true)
                    .build());
        } else if (activityCode.equals("BADMINTON") || activityCode.equals("TENNIS") || activityCode.equals("PADEL")) {
            // Peak hours for courts (6 PM - 10 PM on Weekdays)
            rules.add(ResourcePriceRule.builder()
                    .resourceSlotConfig(config)
                    .dayType(DayType.WEEKDAY)
                    .startTime(LocalTime.of(18, 0))
                    .endTime(LocalTime.of(22, 0))
                    .extraCharge(100.0)
                    .reason("Peak Hour Surcharge")
                    .priority(1)
                    .enabled(true)
                    .build());
        } else if (activityCode.equals("SWIMMING")) {
            // Weekend morning peak (6 AM - 10 AM)
            rules.add(ResourcePriceRule.builder()
                    .resourceSlotConfig(config)
                    .dayType(DayType.WEEKEND)
                    .startTime(LocalTime.of(6, 0))
                    .endTime(LocalTime.of(10, 0))
                    .extraCharge(150.0)
                    .reason("Morning Peak Charge")
                    .priority(1)
                    .enabled(true)
                    .build());
        } else if (activityCode.equals("BOWLING") || activityCode.equals("ARCADE")) {
            // Weekend evening peak (5 PM - 11 PM)
            rules.add(ResourcePriceRule.builder()
                    .resourceSlotConfig(config)
                    .dayType(DayType.WEEKEND)
                    .startTime(LocalTime.of(17, 0))
                    .endTime(LocalTime.of(23, 0))
                    .extraCharge(50.0)
                    .reason("Weekend Evening Peak")
                    .priority(1)
                    .enabled(true)
                    .build());
        }

        if (!rules.isEmpty()) {
            resourcePriceRuleRepository.saveAll(rules);
            log.debug("Created {} price rules for resource: {}", rules.size(), config.getResource().getName());
        }
    }

    /**
     * Helper method to create a ServiceResource
     */
    private ServiceResource createResource(Service service, String name, String description, List<Activity> activities) {
        return ServiceResource.builder()
                .service(service)
                .name(name)
                .description(description)
                .activities(activities)
                .enabled(true)
                .build();
    }

    // COMMENTED OUT: Helper methods for sample data initialization
    /*
    private void assignActivitiesToService(Service service, String activityCode) {
        List<Activity> activities = new ArrayList<>();
        activityRepository.findByCode(activityCode).ifPresent(activities::add);

        // Add secondary activities for some types
        if (activityCode.equals("FOOTBALL")) {
            activityRepository.findByCode("CRICKET").ifPresent(activities::add);
        } else if (activityCode.equals("CRICKET")) {
            activityRepository.findByCode("FOOTBALL").ifPresent(activities::add);
        }

        service.setActivities(activities);
        serviceRepository.save(service);
    }

    private List<String> getAmenitiesForActivity(String activityCode) {
        return switch (activityCode) {

            case "FOOTBALL", "CRICKET" -> List.of(
                    "Flood Lights",
                    "Changing Room",
                    "Washroom",
                    "Parking",
                    "Drinking Water",
                    "Seating Area",
                    "Equipment Rental"
            );

            case "BOWLING" -> List.of(
                    "Air Conditioning",
                    "Shoe Rental",
                    "Food & Beverages",
                    "Restroom",
                    "Waiting Lounge",
                    "Score Display"
            );

            case "BADMINTON", "TENNIS", "PADEL", "BASKETBALL" -> List.of(
                    "Indoor Courts",
                    "Changing Room",
                    "Washroom",
                    "Parking",
                    "Drinking Water",
                    "Seating Area"
            );

            case "SWIMMING" -> List.of(
                    "Changing Room",
                    "Locker Facility",
                    "Shower",
                    "Lifeguard",
                    "Filtered Water",
                    "Parking"
            );

            case "GYM" -> List.of(
                    "Air Conditioning",
                    "Locker Facility",
                    "Shower",
                    "Personal Trainer",
                    "Parking",
                    "Music System"
            );

            case "ARCADE" -> List.of(
                    "Air Conditioning",
                    "Food Court",
                    "Restroom",
                    "Waiting Area",
                    "Kids Friendly"
            );

            case "SPA" -> List.of(
                    "Air Conditioning",
                    "Private Rooms",
                    "Shower",
                    "Towels Provided",
                    "Parking",
                    "Relaxation Area"
            );

            case "STUDIO" -> List.of(
                    "Soundproofing",
                    "Air Conditioning",
                    "Waiting Area",
                    "Power Backup",
                    "Parking"
            );

            case "CONFERENCE" -> List.of(
                    "Projector",
                    "WiFi",
                    "Air Conditioning",
                    "Whiteboard",
                    "Parking",
                    "Power Backup"
            );

            case "PARTY_HALL" -> List.of(
                    "Air Conditioning",
                    "Stage",
                    "Sound System",
                    "Parking",
                    "Power Backup",
                    "Catering Area"
            );

            default -> List.of(
                    "Parking",
                    "Washroom"
            );
        };
    }
    */

    /**
     * Add a simple weekend extra-price rule for a specific resource (by resource id).
     * If the resource or its slot configuration is missing the method will log and return.
     */
    /*
    private void addWeekendExtraPriceRuleForResource(Long resourceId, Double extraCharge) {
        try {
            // Find the resource slot config
            resourceSlotConfigRepository.findByResourceIdAndEnabledTrue(resourceId).ifPresent(config -> {
                // Create a weekend rule that applies for the full day
                ResourcePriceRule rule = ResourcePriceRule.builder()
                        .resourceSlotConfig(config)
                        .dayType(DayType.WEEKEND)
                        .startTime(LocalTime.of(0, 0))
                        .endTime(LocalTime.of(23, 59))
                        .extraCharge(extraCharge)
                        .reason("Weekend extra charge")
                        .priority(1)
                        .enabled(true)
                        .build();

                resourcePriceRuleRepository.save(rule);
                log.info("Added weekend extra price rule for resource id {} with extra charge {}", resourceId, extraCharge);
            });
        } catch (Exception ex) {
            log.warn("Failed to add weekend price rule for resource {}: {}", resourceId, ex.getMessage());
        }
    }
    */

    /**
     * Initialize manager user
     */
    private void initializeManagerUser() {
        try {
            log.info("🏗️ Creating manager user...");

            User manager = User.builder()
                    .email("gethyperadmin@gmail.com")
                    .name("Hyper Manager")
                    .phone("+919876543200")
                    .role(Role.MANAGER)
                    .enabled(true)
                    .build();

            manager = userRepository.save(manager);
            log.info("✅ Created manager user: {} with email: {}", manager.getName(), manager.getEmail());

        } catch (Exception e) {
            log.error("❌ Failed to create manager user: {}", e.getMessage());
        }
    }
}