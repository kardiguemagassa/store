package com.store.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Classe de configuration permettant d'activer l'audit JPA et de fournir les beans nécessaires
 * à la fonctionnalité d'audit.
 * Cette classe utilise l'annotation `@EnableJpaAuditing` pour activer les fonctionnalités d'audit
 * de Spring Data JPA. Elle inclut une implémentation personnalisée de l'interface
 * `AuditorAware` qui détermine l'auditeur actuel pour l'attribution des métadonnées d'audit aux entités persistantes.
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-10-01
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    /**
     * Fourni un bean Spring pour l'interface `AuditorAware` afin de déterminer l'auditeur actuel
     * à des fins d'audit JPA. Ce bean est utilisé par la configuration d'audit pour attribuer automatiquement
     * des métadonnées d'audit, telles que createBy et lastModifiedBy, lors de la persistance des entités.
     * @return une implémentation de `AuditorAware` qui détermine l'auditeur actuel, principalement
     * en fonction du contexte de sécurité. Par défaut, l'utilisateur système est utilisé si aucun auditeur ne peut être déterminé.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }
}