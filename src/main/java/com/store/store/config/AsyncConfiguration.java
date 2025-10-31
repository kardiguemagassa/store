package com.store.store.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration pour l'exécution asynchrone des tâches.
 * Permet d'utiliser @Async dans les services (ex: envoi d'emails).
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Configuration
@EnableAsync  // ← Active le support @Async dans toute l'application
public class AsyncConfiguration implements AsyncConfigurer {

    /**
     * Configure l'executor pour les méthodes @Async.
     *
     * Configuration optimisée pour l'envoi d'emails:
     * - Core pool size: 2 threads (minimum actifs)
     * - Max pool size: 5 threads (maximum en cas de charge)
     * - Queue capacity: 100 tâches en attente
     *
     * Cela signifie:
     * - Jusqu'à 2 emails peuvent être envoyés simultanément
     * - Jusqu'à 5 threads créés si nécessaire sous forte charge
     * - 100 emails peuvent attendre en file d'attente
     *
     * @return L'executor configuré pour les tâches asynchrones
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