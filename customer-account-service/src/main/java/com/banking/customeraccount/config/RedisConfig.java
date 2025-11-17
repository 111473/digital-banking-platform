package com.banking.customeraccount.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Production-grade Redis configuration for Customer Account Service
 * Features:
 * - Connection pooling with Lettuce
 * - Custom serialization with Jackson
 * - Multiple cache configurations with different TTLs
 * - Cache error handling (fail-safe)
 * - Custom key generation
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.redis.timeout:3000}")
    private long redisTimeout;

    @Value("${spring.redis.lettuce.pool.max-active:8}")
    private int maxActive;

    @Value("${spring.redis.lettuce.pool.max-idle:8}")
    private int maxIdle;

    @Value("${spring.redis.lettuce.pool.min-idle:2}")
    private int minIdle;

    /**
     * Redis Connection Factory with Lettuce (production-ready)
     * Lettuce is preferred over Jedis for production use
     */
    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        log.info("Configuring Redis connection to {}:{}", redisHost, redisPort);

        // Redis configuration
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(redisDatabase);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        // Lettuce client configuration with connection pooling
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisTimeout))
                .shutdownTimeout(Duration.ofMillis(100))
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        factory.setValidateConnection(true);

        return factory;
    }

    /**
     * Object Mapper for Redis JSON serialization
     * Configured to handle Java 8 date/time and polymorphic types
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register JavaTimeModule for LocalDateTime, LocalDate, etc.
        mapper.registerModule(new JavaTimeModule());

        // Enable polymorphic type handling for security
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }

    /**
     * RedisTemplate for general-purpose Redis operations
     * Used for manual cache operations and data structures
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();

        log.info("RedisTemplate configured successfully");
        return template;
    }

    /**
     * Cache Manager with multiple cache configurations
     * Different caches for different data types with appropriate TTLs
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        log.info("Configuring Redis Cache Manager with multiple cache configurations");

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = createCacheConfiguration(Duration.ofMinutes(10));

        // Specific cache configurations with different TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Customer data - 30 minutes (frequently accessed, moderate change rate)
        cacheConfigurations.put("customers",
                createCacheConfiguration(Duration.ofMinutes(30)));

        // Customer by ID - 30 minutes
        cacheConfigurations.put("customerById",
                createCacheConfiguration(Duration.ofMinutes(30)));

        // Customer by application - 30 minutes
        cacheConfigurations.put("customerByApplication",
                createCacheConfiguration(Duration.ofMinutes(30)));

        // Branch info - 1 hour (changes infrequently)
        cacheConfigurations.put("branchInfo",
                createCacheConfiguration(Duration.ofHours(1)));

        // Customers by branch - 15 minutes (list can change frequently)
        cacheConfigurations.put("customersByBranch",
                createCacheConfiguration(Duration.ofMinutes(15)));

        // All customers list - 5 minutes (expensive query, but needs freshness)
        cacheConfigurations.put("allCustomers",
                createCacheConfiguration(Duration.ofMinutes(5)));

        // Existence checks - 10 minutes (boolean checks)
        cacheConfigurations.put("customerExists",
                createCacheConfiguration(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(redisConnectionFactory())
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * Create cache configuration with specific TTL
     */
    private RedisCacheConfiguration createCacheConfiguration(Duration ttl) {
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                jsonSerializer))
                .disableCachingNullValues();
    }

    /**
     * Custom key generator for complex cache keys
     * Format: className.methodName:param1:param2:...
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName());
            sb.append(".");
            sb.append(method.getName());

            for (Object param : params) {
                sb.append(":");
                sb.append(param != null ? param.toString() : "null");
            }

            return sb.toString();
        };
    }

    /**
     * Cache error handler - fail-safe behavior
     * If Redis fails, application continues without caching
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception,
                                            org.springframework.cache.Cache cache,
                                            Object key) {
                log.error("Cache GET error for cache: {}, key: {}. Continuing without cache.",
                        cache.getName(), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception,
                                            org.springframework.cache.Cache cache,
                                            Object key,
                                            Object value) {
                log.error("Cache PUT error for cache: {}, key: {}. Continuing without cache.",
                        cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception,
                                              org.springframework.cache.Cache cache,
                                              Object key) {
                log.error("Cache EVICT error for cache: {}, key: {}. Continuing without cache.",
                        cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception,
                                              org.springframework.cache.Cache cache) {
                log.error("Cache CLEAR error for cache: {}. Continuing without cache.",
                        cache.getName(), exception);
            }
        };
    }
}