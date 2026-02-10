package com.hitendra.turf_booking_backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    /**
     * Shared ExecutorService bean for parallel image uploads in CloudinaryService.
     * Using a fixed thread pool of 4 threads (max images that can be uploaded at once).
     * This is a singleton bean shared across the application to avoid thread proliferation.
     * Threads are non-daemon to ensure uploads complete before JVM shutdown.
     * The @PreDestroy hook in CloudinaryService ensures graceful termination.
     */
    @Bean(name = "imageUploadExecutor")
    public ExecutorService imageUploadExecutor() {
        return Executors.newFixedThreadPool(4, r -> {
            Thread thread = new Thread(r);
            thread.setName("image-upload-" + thread.getId());
            thread.setDaemon(false); // Ensure uploads complete before shutdown
            return thread;
        });
    }
}
