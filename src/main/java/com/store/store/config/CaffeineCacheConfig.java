package com.store.store.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Configuration class for setting up caching using Caffeine as the caching provider.
 *
 * The class is annotated with `@Configuration` to indicate that it serves as a source
 * of bean definitions and with `@EnableCaching` to enable the caching functionality
 * provided by Spring. It defines multiple caches with specific configurations, such as
 * expiration times and maximum allowable entries, to optimize performance and resource usage.
 *
 * The cache configurations include:
 * - `product`: Cache for individual products with a 30-minute expiration time and a maximum size of 500 entries.
 * - `products`: Cache for product lists with a 15-minute expiration time and a maximum size of 100 entries.
 * - `productsByCategory`: Cache for products by category with a 20-minute expiration time and a maximum size of 200 entries.
 * - `categories`: Cache for product categories with a 2-hour expiration time and a maximum size of 50 entries.
 * - `roles`: Cache for roles with a 1-day expiration time and a maximum size of 1 entry.
 *
 * The configured caches are managed by `SimpleCacheManager`, which aggregates all the defined caches
 * for automatic handling of cache operations.
 *
 * This class ensures efficient and tailored caching for different use cases within the system
 * to reduce redundant computations and improve performance.
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-10-01
 */
@Configuration
@EnableCaching
public class CaffeineCacheConfig {

    /**
     * Configures and returns a CacheManager implementation using Caffeine as the caching provider.
     * Defines multiple caches with specific expiration times and maximum sizes for different use cases.
     * The caches include:
     * - A cache for individual products, expiring after 30 minutes with a maximum size of 500 entries.
     * - A cache for product lists, expiring after 15 minutes with a maximum size of 100 entries.
     * - A cache for products by category, expiring after 20 minutes with a maximum size of 200 entries.
     * - A cache for categories, expiring after 2 hours with a maximum size of 50 entries.
     * - A cache for roles, expiring after 1 day with a maximum size of 1 entry.
     *
     * @return a configured {@link CacheManager} instance managing the defined Caffeine caches.
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        // Cache pour les produits individuels
        CaffeineCache productCache = new CaffeineCache("product",
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .build());

        // Cache pour les listes de produits
        CaffeineCache productsCache = new CaffeineCache("products",
                Caffeine.newBuilder()
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .build());

        // ✅ AJOUT: Cache pour les produits par catégorie
        CaffeineCache productsByCategoryCache = new CaffeineCache("productsByCategory",
                Caffeine.newBuilder()
                        .expireAfterWrite(20, TimeUnit.MINUTES)
                        .maximumSize(200) // Plus grand car plusieurs catégories
                        .build());

        // Cache pour les catégories
        CaffeineCache categoriesCache = new CaffeineCache("categories",
                Caffeine.newBuilder()
                        .expireAfterWrite(2, TimeUnit.HOURS)
                        .maximumSize(50)
                        .build());

        CaffeineCache rolesCache = new CaffeineCache("roles",
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.DAYS)
                        .maximumSize(1)
                        .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(
                productCache,
                productsCache,
                productsByCategoryCache, // ✅ AJOUT
                categoriesCache,
                rolesCache
        ));
        return manager;
    }
}