package com.store.store.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe de configuration pour activer les fonctionnalités de planification de Spring.
 * Cette classe est annotée avec `@Configuration` pour la désigner comme source
 * de définitions de beans, et `@EnableScheduling` pour activer la planification des tâches
 * à l'aide d'annotations telles que `@Scheduled` dans l'application.
 * La configuration permet à l'application de définir et d'exécuter efficacement des tâches périodiques
 * ou planifiées.
 * @author Kardigué
 * @version 1.0
 * @since 2025-01-27
 */
@Configuration
@EnableScheduling
public class SchedulingConfig { }
