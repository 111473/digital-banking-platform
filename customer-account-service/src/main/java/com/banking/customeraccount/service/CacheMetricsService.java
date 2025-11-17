package com.banking.customeraccount.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Service for monitoring and managing cache metrics
 * Provides insights into cache performance
 */
@Slf4j
@Service
public class CacheMetricsService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    // Track cache statistics
    private final Map<String, CacheStats> cacheStatsMap = new HashMap<>();

    public CacheMetricsService(CacheManager cacheManager,
                               RedisTemplate<String, Object> redisTemplate) {
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Get cache statistics for all caches
     */
    public Map<String, CacheStats> getAllCacheStats() {
        Map<String, CacheStats> stats = new HashMap<>();

        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                CacheStats cacheStats = cacheStatsMap.getOrDefault(
                        cacheName,
                        new CacheStats(cacheName)
                );
                stats.put(cacheName, cacheStats);
            }
        }

        return stats;
    }

    /**
     * Get Redis memory usage and key count
     */
    public Map<String, Object> getRedisStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Get all keys count (expensive operation - use with caution in production)
            Long dbSize = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .dbSize();

            stats.put("totalKeys", dbSize);
            stats.put("status", "UP");

            // Get memory info
            java.util.Properties info = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .info("memory");

            stats.put("usedMemory", info.getProperty("used_memory_human"));
            stats.put("maxMemory", info.getProperty("maxmemory_human"));

        } catch (Exception e) {
            log.error("Failed to get Redis stats", e);
            stats.put("status", "ERROR");
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * Clear specific cache
     */
    public void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cleared cache: {}", cacheName);
        } else {
            log.warn("Cache not found: {}", cacheName);
        }
    }

    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        for (String cacheName : cacheManager.getCacheNames()) {
            clearCache(cacheName);
        }
        log.info("Cleared all caches");
    }

    /**
     * Get keys in a specific cache (for debugging)
     */
    public Set<String> getCacheKeys(String cachePrefix) {
        try {
            return redisTemplate.keys(cachePrefix + "*");
        } catch (Exception e) {
            log.error("Failed to get cache keys for prefix: {}", cachePrefix, e);
            return Set.of();
        }
    }

    /**
     * Periodic cache statistics logging
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void logCacheStatistics() {
        log.info("=== Cache Statistics ===");

        Map<String, Object> redisStats = getRedisStats();
        log.info("Redis Status: {}, Total Keys: {}, Used Memory: {}",
                redisStats.get("status"),
                redisStats.get("totalKeys"),
                redisStats.get("usedMemory"));

        Map<String, CacheStats> allStats = getAllCacheStats();
        for (Map.Entry<String, CacheStats> entry : allStats.entrySet()) {
            CacheStats stats = entry.getValue();
            log.info("Cache '{}': Hits={}, Misses={}, Hit Rate={}%",
                    stats.getCacheName(),
                    stats.getHits(),
                    stats.getMisses(),
                    stats.getHitRate());
        }

        log.info("========================");
    }

    /**
     * Cache statistics holder
     */
    public static class CacheStats {
        private final String cacheName;
        private long hits = 0;
        private long misses = 0;

        public CacheStats(String cacheName) {
            this.cacheName = cacheName;
        }

        public void recordHit() {
            hits++;
        }

        public void recordMiss() {
            misses++;
        }

        public String getCacheName() {
            return cacheName;
        }

        public long getHits() {
            return hits;
        }

        public long getMisses() {
            return misses;
        }

        public double getHitRate() {
            long total = hits + misses;
            return total == 0 ? 0 : (hits * 100.0) / total;
        }
    }
}