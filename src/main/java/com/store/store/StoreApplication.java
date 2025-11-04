package com.store.store;

import com.store.store.dto.ContactInfoDto;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Main entry point of the Store application.
 *
 * This class configures and starts the Spring Boot application.
 * It includes specific configurations to enable caching and support
 * external configuration properties.
 *
 * Annotations used in this class:
 * - {@code @SpringBootApplication}: Indicates this is a Spring Boot application
 *   and triggers auto-configuration, component scanning, and additional Spring Boot features.
 * - {@code @EnableCaching}: Enables Spring's annotation-driven caching mechanism.
 * - {@code @EnableConfigurationProperties}: Registers configuration property classes
 *   to enable externalized configuration; in this case, {@code ContactInfoDto}.
 */
@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(value = {ContactInfoDto.class})
public class StoreApplication { public static void main(String[] args) {SpringApplication.run(StoreApplication.class, args);}
}