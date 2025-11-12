package com.store.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready
 * @since 2025-01-06
 */
@Configuration
public class LocaleConfiguration implements WebMvcConfigurer {


    // CONSTANTES - LOCALES SUPPORTÉES
    private static final Locale DEFAULT_LOCALE = Locale.FRENCH;


    private static final List<Locale> SUPPORTED_LOCALES = Arrays.asList(
            Locale.FRENCH,       // fr - Français (défaut)
            Locale.ENGLISH,      // en - Anglais
            new Locale("es")     // es - Espagnol
            // Ajouter d'autres langues ici si nécessaire :
            // new Locale("pt"),  // pt - Portugais
            // new Locale("de"),  // de - Allemand
            // new Locale("it"),  // it - Italien
            // new Locale("ar")   // ar - Arabe
    );


    // CONFIGURATION - LOCALE RESOLVER
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();

        // Locale par défaut (fallback)
        resolver.setDefaultLocale(DEFAULT_LOCALE);

        // Locales supportées
        resolver.setSupportedLocales(SUPPORTED_LOCALES);

        return resolver;
    }

    // CONFIGURATION - LOCALE CHANGE INTERCEPTOR
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang"); // Nom du paramètre URL
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
