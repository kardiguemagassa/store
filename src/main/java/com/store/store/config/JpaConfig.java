package com.store.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration class for enabling JPA auditing and providing necessary
 * beans for auditing functionality.
 *
 * This class uses the `@EnableJpaAuditing` annotation to enable the auditing
 * features of Spring Data JPA. It includes a custom implementation of the
 * `AuditorAware` interface that determines the current auditor for assigning
 * audit-related metadata in persistable entities.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    /**
     * Provides a Spring bean for the `AuditorAware` interface to determine the current auditor
     * for JPA auditing purposes. This bean is used by the auditing configuration to automatically
     * assign auditing metadata, such as createdBy and lastModifiedBy, during entity persistence.
     *
     * @return an implementation of `AuditorAware` that determines the current auditor, primarily
     *         based on the security context. Defaults to the system user when no auditor can be determined.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }
}