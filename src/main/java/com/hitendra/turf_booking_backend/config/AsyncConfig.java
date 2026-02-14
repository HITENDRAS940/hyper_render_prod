package com.hitendra.turf_booking_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for async task execution.
 * Used by @Async annotated methods (e.g., InvoiceGenerationEventListener).
 *
 * CRITICAL: Without this config, @Async might use the default SimpleAsyncTaskExecutor
 * which creates a new thread for each task and doesn't have proper error handling.
 *
 * This config ensures:
 * - Proper thread pool management
 * - Named threads for debugging
 * - Error handling for uncaught exceptions
 * - Graceful shutdown
 */
@Configuration
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Value("${spring.task.execution.pool.core-size:2}")
    private int corePoolSize;

    @Value("${spring.task.execution.pool.max-size:4}")
    private int maxPoolSize;

    @Value("${spring.task.execution.pool.queue-capacity:100}")
    private int queueCapacity;

    @Value("${spring.task.execution.thread-name-prefix:async-}")
    private String threadNamePrefix;

    /**
     * Configure the async executor for @Async methods.
     * This executor is used by InvoiceGenerationEventListener.
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("✅ Async executor configured: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                corePoolSize, maxPoolSize, queueCapacity);

        return executor;
    }

    /**
     * Handle uncaught exceptions in async methods.
     * Logs errors instead of silently failing.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("❌ Uncaught exception in async method: {}.{}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    throwable);
        };
    }
}

