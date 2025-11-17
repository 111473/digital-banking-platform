package com.banking.customeraccount.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for Redis
 * Provides detailed health information for monitoring
 */
@Slf4j
@Component("redis")
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory connectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try {
            RedisConnection connection = connectionFactory.getConnection();

            // Ping Redis
            String pong = connection.ping();

            // Get Redis info
            java.util.Properties info = connection.info();
            String version = info.getProperty("redis_version");
            String mode = info.getProperty("redis_mode");
            String usedMemory = info.getProperty("used_memory_human");
            String connectedClients = info.getProperty("connected_clients");

            connection.close();

            return Health.up()
                    .withDetail("ping", pong)
                    .withDetail("version", version)
                    .withDetail("mode", mode)
                    .withDetail("usedMemory", usedMemory)
                    .withDetail("connectedClients", connectedClients)
                    .build();

        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}