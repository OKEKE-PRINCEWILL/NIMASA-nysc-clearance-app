//package com.example.NIMASA.NYSC.Clearance.Form.config;
//
//import org.springframework.cache.CacheManager;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
//import java.util.Arrays;
//import java.util.concurrent.Executor;
//
//@Configuration
//@EnableAsync
//@EnableCaching
//public class PerformanceConfig {
//
//    /**
//     * Async executor for background tasks (like rate limiting)
//     * This prevents blocking the main authentication thread
//     */
//    @Bean(name = "authAsyncExecutor")
//    public Executor authAsyncExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(2);        // Minimum threads
//        executor.setMaxPoolSize(5);         // Maximum threads
//        executor.setQueueCapacity(50);      // Queue size
//        executor.setThreadNamePrefix("AuthAsync-");
//        executor.setKeepAliveSeconds(60);   // Thread idle time
//        executor.initialize();
//        return executor;
//    }
//
//    /**
//     * Cache manager for frequently accessed data
//     * Reduces database queries for employee lists and form counts
//     */
//    @Bean
//    public CacheManager cacheManager() {
//        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
//
//        // Define cache names
//        cacheManager.setCacheNames(Arrays.asList(
//                "employeeList",        // Cache employee list
//                "pendingFormsCount",   // Cache pending forms count per employee
//                "userSessions"         // Cache user session data
//        ));
//
//        // Allow dynamic cache creation
//        cacheManager.setAllowNullValues(false);
//
//        return cacheManager;
//    }
//
//    /**
//     * Connection pool optimization for database
//     */
//    @Bean
//    public DatabaseOptimizationConfig databaseOptimizationConfig() {
//        return new DatabaseOptimizationConfig();
//    }
//
//    public static class DatabaseOptimizationConfig {
//        // These would be applied to your DataSource configuration
//        public int getMaximumPoolSize() { return 10; }
//        public int getMinimumIdle() { return 2; }
//        public long getConnectionTimeout() { return 20000; } // 20 seconds
//        public long getIdleTimeout() { return 300000; }      // 5 minutes
//        public long getMaxLifetime() { return 1200000; }     // 20 minutes
//    }
//}