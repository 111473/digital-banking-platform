package com.banking.customeraccount.controller;

import com.banking.customeraccount.service.CacheMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * Controller for cache management and monitoring
 * Useful for operations and debugging
 *
 * SECURITY NOTE: In production, secure these endpoints with proper authentication
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
public class CacheManagementController {

    private final CacheMetricsService cacheMetricsService;

    public CacheManagementController(CacheMetricsService cacheMetricsService) {
        this.cacheMetricsService = cacheMetricsService;
    }

    /**
     * Get all cache statistics
     * GET /api/cache/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, CacheMetricsService.CacheStats>> getCacheStats() {
        log.info("Fetching cache statistics");
        return ResponseEntity.ok(cacheMetricsService.getAllCacheStats());
    }

    /**
     * Get Redis statistics
     * GET /api/cache/redis-stats
     */
    @GetMapping("/redis-stats")
    public ResponseEntity<Map<String, Object>> getRedisStats() {
        log.info("Fetching Redis statistics");
        return ResponseEntity.ok(cacheMetricsService.getRedisStats());
    }

    /**
     * Clear specific cache
     * DELETE /api/cache/{cacheName}
     */
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<Map<String, String>> clearCache(@PathVariable String cacheName) {
        log.info("Clearing cache: {}", cacheName);
        cacheMetricsService.clearCache(cacheName);
        return ResponseEntity.ok(Map.of(
                "message", "Cache cleared successfully",
                "cacheName", cacheName
        ));
    }

    /**
     * Clear all caches
     * DELETE /api/cache/all
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        log.info("Clearing all caches");
        cacheMetricsService.clearAllCaches();
        return ResponseEntity.ok(Map.of(
                "message", "All caches cleared successfully"
        ));
    }

    /**
     * Get cache keys (for debugging)
     * GET /api/cache/keys?prefix=customerById
     */
    @GetMapping("/keys")
    public ResponseEntity<Set<String>> getCacheKeys(@RequestParam String prefix) {
        log.info("Fetching cache keys for prefix: {}", prefix);
        Set<String> keys = cacheMetricsService.getCacheKeys(prefix);
        return ResponseEntity.ok(keys);
    }

    /**
     * Health check for cache system
     * GET /api/cache/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "cacheManager", "active",
                "redisStats", cacheMetricsService.getRedisStats()
        );
        return ResponseEntity.ok(health);
    }
}