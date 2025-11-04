package com.store.store.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration class for enabling and customizing asynchronous processing.
 *
 * This class utilizes Spring's {@code @EnableAsync} annotation to activate support for
 * {@code @Async} throughout the application. The asynchronous tasks are executed using a
 * custom-configured thread pool executor.
 *
 * Key Characteristics of the Configuration:
 * - Core Pool Size: A minimum of 2 threads are active at all times.
 * - Maximum Pool Size: Up to 5 threads can be created during high load.
 * - Queue Capacity: A total of 100 tasks can wait in line if all threads are occupied.
 *
 * The thread executor is optimized for handling tasks such as sending emails with
 * efficient resource usage and queue management. Additionally, the threads are prefixed
 * with "AsyncEmail-" for better debug logging and traceability.
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-10-01
 */
@Slf4j
@Configuration
@EnableAsync  // ← Active le support @Async dans toute l'application
public class AsyncConfiguration implements AsyncConfigurer {

    /**
     * Provides a customized {@link Executor} implementation for asynchronous processing.
     *
     * The executor is configured with specific parameters for efficient handling of
     * concurrent tasks:
     * - Core pool size is set to 2, ensuring a minimum of 2 threads are always active.
     * - Maximum pool size is set to 5, allowing up to 5 threads to handle tasks during high load.
     * - Queue capacity is set to 100, enabling the executor to hold up to 100 tasks in the queue if all threads are busy.
     * - Thread names are prefixed with "AsyncEmail-" for better debugging and identification in log outputs.
     *
     * The executor is ideal for managing tasks like email sending or background data processing
     * in the application.
     *
     * @return a fully configured {@link ThreadPoolTaskExecutor} instance for asynchronous task execution
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Nombre de threads minimum toujours actifs
        executor.setCorePoolSize(2);

        // Nombre maximum de threads qui peuvent être créés
        executor.setMaxPoolSize(5);

        // Taille de la file d'attente pour les tâches en attente
        executor.setQueueCapacity(100);

        // Préfixe des noms de threads (pour debugging dans les logs)
        executor.setThreadNamePrefix("AsyncEmail-");

        // Initialiser l'executor
        executor.initialize();

        log.info("Async executor configured: corePoolSize=2, maxPoolSize=5, queueCapacity=100");

        return executor;
    }
}