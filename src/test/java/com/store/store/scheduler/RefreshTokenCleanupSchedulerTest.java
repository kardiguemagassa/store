package com.store.store.scheduler;

import com.store.store.entity.RefreshToken;
import com.store.store.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;

import static org.junit.jupiter.api.Assertions.assertFalse;


/*    *
        * <p><strong>Logs générés :</strong></p>
        * <pre>
 * // Début du job
         * INFO  - Starting scheduled refresh token cleanup job
 * INFO  - Starting cleanup of expired refresh tokens
 *
         * // Nettoyage dans RefreshTokenService
         * // (suppression des tokens expirés depuis > 30 jours)
         *
         * // Fin du job
         * INFO  - Expired refresh tokens cleanup completed
 * INFO  - Scheduled refresh token cleanup job completed successfully
 * </pre>
        *
        * <p><strong>Monitoring et métriques recommandés :</strong></p>
        * <ul>
 *   <li>Nombre de tokens supprimés à chaque exécution</li>
        *   <li>Durée d'exécution du job</li>
        *   <li>Nombre total de tokens restants après nettoyage</li>
        *   <li>Alertes si job échoue (erreur base de données)</li>
        *   <li>Alertes si trop de tokens supprimés (anomalie)</li>
        * </ul>
        *
        * <p><strong>Tests recommandés :</strong></p>
        * <pre>
        */
@SpringBootTest
class RefreshTokenCleanupSchedulerTest {

    @Autowired
    private RefreshTokenRepository repository;

    @Autowired
    private RefreshTokenCleanupScheduler scheduler;

    @Test
    void cleanupJob_DeletesOldExpiredTokens() {
          // Given: Token expiré depuis 60 jours
          RefreshToken oldToken = createExpiredToken(60);
          repository.save(oldToken);

          // When: Job exécuté
          scheduler.cleanupExpiredRefreshTokens();
         // Then: Token supprimé
         assertFalse(repository.existsById(oldToken.getId()));
    }

        @Test
      void cleanupJob_KeepsRecentExpiredTokens() {
          // Given: Token expiré depuis 10 jours (< 30 jours)
          RefreshToken recentToken = createExpiredToken(10);
          repository.save(recentToken);

          // When: Job exécuté
          scheduler.cleanupExpiredRefreshTokens();

          // Then: Token conservé
          assertTrue(repository.existsById(recentToken.getId()));
     }
     @Test
      void cleanupJob_KeepsActiveTokens() {
         // Given: Token actif (non expiré)
         RefreshToken activeToken = createActiveToken();
         repository.save(activeToken);

         // When: Job exécuté
          scheduler.cleanupExpiredRefreshTokens();

          // Then: Token conservé
          assertTrue(repository.existsById(activeToken.getId()));
      }
  }