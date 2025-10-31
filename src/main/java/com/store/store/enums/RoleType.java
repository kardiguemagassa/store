package com.store.store.enums;

/**
 * Énumération des types de rôles pour l'e-commerce.
 *
 * @author Kardigué
 * @version 2.0 - E-commerce roles
 * @since 2025-01-27
 */
public enum RoleType {
    /**
     * Client de la boutique - peut passer des commandes, consulter son historique et gérer son profil
     */
    ROLE_USER("Client", 1),

    /**
     * Employé de la boutique - peut consulter les commandes, répondre aux messages clients et mettre à jour les statuts de livraison
     */
    ROLE_EMPLOYEE("Employé", 2),

    /**
     * Gestionnaire de boutique - peut gérer les produits, traiter les commandes, voir les rapports de vente et contacter les clients
     */
    ROLE_MANAGER("Gestionnaire", 3),

    /**
     * Administrateur système - accès complet à toutes les fonctionnalités, gestion des utilisateurs, configuration système et rapports avancés
     */
    ROLE_ADMIN("Administrateur", 5);

    private final String displayName;
    private final int level;

    RoleType(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public boolean isHigherThan(RoleType other) {
        return this.level > other.level;
    }

    public boolean isAdmin() {
        return this == ROLE_ADMIN;
    }

    public boolean isUser() {
        return this == ROLE_USER;
    }

    public boolean isManager() {
        return this == ROLE_MANAGER;
    }

    public boolean isEmployee() {
        return this == ROLE_EMPLOYEE;
    }
}