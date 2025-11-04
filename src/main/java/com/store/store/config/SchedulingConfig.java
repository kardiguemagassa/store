package com.store.store.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class for enabling Spring's scheduling capabilities.
 *
 * This class is annotated with `@Configuration` to mark it as a source
 * of bean definitions, and `@EnableScheduling` to enable scheduling tasks
 * using annotations such as `@Scheduled` in the application.
 *
 * The configuration allows the application to define and execute periodic
 * or scheduled tasks efficiently.
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-01-27
 */
@Configuration
@EnableScheduling
public class SchedulingConfig { }
