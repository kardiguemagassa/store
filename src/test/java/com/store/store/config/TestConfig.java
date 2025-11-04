/*package com.store.store.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Slf4j
@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public CommandLineRunner testInitialAdminCreator() {
        return args -> {
            log.info("🔧 InitialAdminCreator désactivé pour les tests unitaires");
            // Ne pas créer de données initiales pendant les tests
        };
    }
}*/

