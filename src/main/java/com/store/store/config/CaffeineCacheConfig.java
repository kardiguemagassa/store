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
 * @author Kardigué
 * @version 1.0
 * @since 2025-10-01
 */
@Configuration
@EnableCaching
public class CaffeineCacheConfig {

    /**
     * Configure et retourne une implémentation de CacheManager utilisant Caffeine comme fournisseur de cache.
     * Définit plusieurs caches avec des durées d'expiration et des tailles maximales spécifiques pour différents cas d'utilisation.
     * Les caches incluent:
     * Un cache pour les produits individuels, expirant après 30 minutes et pouvant contenir jusqu'à 500 entrées.
     * Un cache pour les listes de produits, expirant après 15 minutes et pouvant contenir jusqu'à 100 entrées.
     * Un cache pour les produits par catégorie, expirant après 20 minutes et pouvant contenir jusqu'à 200 entrées.
     * Un cache pour les catégories, expirant après 2 heures et pouvant contenir jusqu'à 50 entrées.
     * Un cache pour les rôles, expirant après 1 jour et pouvant contenir jusqu'à 1 entrée.
     * @return une instance de {@link CacheManager} configurée, gérant les caches Caffeine définis.
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

        // AJOUT: Cache pour les produits par catégorie
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
                productsByCategoryCache, // AJOUT
                categoriesCache,
                rolesCache
        ));
        return manager;
    }
}