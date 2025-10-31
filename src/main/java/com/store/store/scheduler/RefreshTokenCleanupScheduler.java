package com.store.store.scheduler;

import com.store.store.service.IRefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job planifié pour nettoyer les refresh tokens expirés.
 * Supprime automatiquement les tokens expirés selon le cron configuré.
 *
 *  BONNES PRATIQUES APPLIQUÉES:
 * - @Component au lieu de @Configuration (car c'est une tâche planifiée)
 * - ConditionalOnProperty pour activer/désactiver facilement
 * - Gestion des exceptions pour ne pas interrompre le scheduler
 * - Logging détaillé pour le monitoring
 *
 * @author Kardigué
 * @version 2.1 - CORRECTED
 * @since 2025-01-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "store.refresh-token.cleanup-enabled",
        havingValue = "true",
        matchIfMissing = true  // Activé par défaut
)
public class RefreshTokenCleanupScheduler {

    private final IRefreshTokenService refreshTokenService;

    /**
     *  Nettoie les refresh tokens expirés.
     *  Configuration par défaut:
     * - Exécuté tous les jours à 3h du matin (cron: 0 0 3 * * ?)
     * - Configurable via application.yml
     *
     * Pour désactiver: store.refresh-token.cleanup-enabled=false
     * Pour changer l'heure: store.refresh-token.cleanup-cron=0 0 2 * * ?
     */
    @Scheduled(cron = "${store.refresh-token.cleanup-cron:0 0 3 * * ?}")
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of expired refresh tokens...");

        try {
            int deletedCount = refreshTokenService.deleteExpiredTokens();
            log.info("Cleanup completed successfully. Deleted {} expired refresh tokens", deletedCount);
        } catch (Exception e) {
            log.error("Error during token cleanup: {}", e.getMessage(), e);
            // Ne pas relancer l'exception pour ne pas arrêter le scheduler
        }
    }
}