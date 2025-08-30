package com.yuksel.investmenttracker.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "portfolio-summary",
                "asset-prices", 
                "portfolio-analytics",
                "asset-allocation",
                "top-movers",
                "portfolio-history"
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5)) // Cache for 5 minutes
                .recordStats());
        
        return cacheManager;
    }

    @Bean("longTermCacheManager")
    public CacheManager longTermCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "asset-details",
                "user-preferences"
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(500)
                .expireAfterWrite(Duration.ofHours(1)) // Cache for 1 hour
                .recordStats());
        
        return cacheManager;
    }

    @Bean("pricesCacheManager")
    public CacheManager pricesCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "current-prices",
                "price-history"
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(200)
                .maximumSize(2000)
                .expireAfterWrite(Duration.ofMinutes(2)) // Prices change frequently
                .recordStats());
        
        return cacheManager;
    }
}