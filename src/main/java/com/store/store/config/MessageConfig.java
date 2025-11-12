package com.store.store.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready
 * @since 2025-01-06
 */
@Configuration
public class MessageConfig {

    /**
     *
     * @return MessageSource configuré pour UTF-8
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

        // Emplacement des fichiers de messages
        messageSource.setBasename("classpath:messages");

        // l'encodage UTF-8
        messageSource.setDefaultEncoding("UTF-8");

        // Cache des messages (production)
        messageSource.setCacheSeconds(3600); // 1 heure

        // Désactiver le fallback vers la locale système
        messageSource.setFallbackToSystemLocale(false);

        // Locale par défaut
        messageSource.setDefaultLocale(java.util.Locale.FRENCH);

        messageSource.setFallbackToSystemLocale(false);
        messageSource.setUseCodeAsDefaultMessage(true);

        return messageSource;
    }
}