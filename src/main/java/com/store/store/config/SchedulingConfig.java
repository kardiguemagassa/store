package com.store.store.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Cette classe active le scheduling pour toute l'application
 * Les tâches planifiées utilisent @Scheduled dans leurs classes respectives
 *
 * - Cette classe active uniquement le scheduling
 * - Les tâches planifiées sont dans le package scheduler
 *
 *  POURQUOI @EnableScheduling ICI ?
 * - Centralise l'activation du scheduling dans la configuration
 * - Facilite les tests (on peut désactiver le scheduling en test)
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-01-27
 */
@Configuration
@EnableScheduling
public class SchedulingConfig { }
