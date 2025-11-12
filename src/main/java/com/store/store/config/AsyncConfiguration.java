package com.store.store.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @author Kardigué
 * @version 1.0
 * @since 2025-10-01
 */
@Slf4j
@Configuration
@EnableAsync  // ← Active le support @Async dans toute l'application
public class AsyncConfiguration implements AsyncConfigurer {

    /**
     * Fournit une implémentation personnalisée de {@link Executor} pour le traitement asynchrone.
     * L'exécuteur est configuré avec des paramètres spécifiques pour une gestion efficace des
     * tâches simultanées:
     * La taille du pool de cœurs est fixée à 2, garantissant qu'au moins 2 threads sont toujours actifs.
     * La taille maximale du pool est fixée à 5, permettant à un maximum de 5 threads de gérer les tâches en cas de forte charge.
     * La capacité de la file d'attente est fixée à 100,
     * permettant à l'exécuteur de contenir jusqu'à 100 tâches dans la file d'attente si tous les threads sont occupés.
     * Les noms des threads sont préfixés par «AsyncEmail-» pour faciliter le débogage et l'identification dans les journaux.
     * L'exécuteur est idéal pour gérer des tâches telles que l'envoi d'e-mails ou le traitement de données en arrière-plan
     * dans l'application.
     * @return une instance de {@link ThreadPoolTaskExecutor} entièrement configurée pour l'exécution de tâches asynchrones
     * Envoyer des commentaires
     * Panneaux latéraux
     * Historique
     * Enregistrées
     **/
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