package com.hitendra.turf_booking_backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for the application.
 * Uses in-memory caching with ConcurrentMapCacheManager for simplicity.
 * 
 * Cached data:
 * - activities: Activity list (rarely changes)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Simple in-memory cache for static/semi-static data
        return new ConcurrentMapCacheManager("activities");
    }
}
