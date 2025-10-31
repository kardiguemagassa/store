package com.store.store.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Entité représentant un Refresh Token stocké en base de données.
 *
 * Fonctionnalités :
 * - Stockage sécurisé des refresh tokens (UUID)
 * - Association avec un Customer
 * - Expiration après 7 jours
 * - Révocation pour sécurité (token rotation)
 * - Tracking IP et User-Agent pour détection de vol
 *
 * @author Kardigué
 * @version 2.1
 * @since 2025-01-27
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_token", columnList = "token"),
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_expiry_date", columnList = "expiry_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Token UUID unique (format: 550e8400-e29b-41d4-a716-446655440000)
     * Stocké en base pour validation côté backend.
     */
    @Column(nullable = false, unique = true, length = 36)
    private String token;

    /**
     * Customer propriétaire du token.
     * Cascade DELETE : Si customer supprimé, tokens supprimés aussi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * Date d'expiration du token.
     * Par défaut : created_date + 7 jours
     * Instant au lieu de LocalDateTime
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Indicateur de révocation.
     * true = token révoqué (rotation ou logout ou replay attack détecté)
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /**
     * Date de création du token.
     * Utiliser @CreationTimestamp avec Instant
     */
    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    /**
     * Mise à jour automatique lors de la révocation.
     */
    @UpdateTimestamp
    @Column(name = "updated_date")
    private Instant updatedDate;

    /**
     * Adresse IP du client lors de la création du token.
     * détecter les vols de tokens (changement d'IP suspect).
     */
    @Column(name = "ip_address", length = 45)  // IPv6 max = 45 caractères
    private String ipAddress;

    /**
     * User-Agent du navigateur lors de la création.
     * Utilisé pour détecter les changements de device.
     */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /**
     * Informations supplémentaires sur le device (optionnel).
     * Peut contenir : OS, type de device, etc.
     */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    /**
     * Vérifie si le token est expiré.
     * Utiliser Instant.now() au lieu de LocalDateTime.now()
     *
     * @return true si expiry_date < maintenant
     */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }

    /**
     * Vérifie si le token est valide (non expiré ET non révoqué).
     *
     * @return true si token utilisable
     */
    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }

    /**
     * Révoque le token.
     * Utilisé lors de : logout, refresh (rotation), replay attack détecté.
     */
    public void revoke() {
        this.revoked = true;
    }
}