package com.store.store.security;

import com.store.store.entity.Customer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implémentation de UserDetails pour l'authentification des customers.
 * Record immutable encapsulant un Customer avec validation.
 *
 * @param customer L'entité Customer (ne doit pas être null)
 * @author Kardigué
 * @version 2.0
 * @since 2025-01-01
 */
public record CustomerUserDetails(Customer customer) implements UserDetails {

    /**
     * Compact constructor avec validation.
     * Vérifie que customer, email et passwordHash sont présents.
     */
    public CustomerUserDetails {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            throw new IllegalArgumentException("Customer email cannot be null or blank");
        }
        if (customer.getPasswordHash() == null || customer.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException("Customer password cannot be null or blank");
        }
    }

    /**
     * Retourne les autorités (rôles) du customer.
     * Gestion défensive des nulls avec filtrage.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Optional.ofNullable(customer.getRoles())
                .orElse(Set.of())
                .stream()
                .filter(Objects::nonNull)
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());
    }

    /**
     * Retourne le hash BCrypt du mot de passe.
     */
    @Override
    public String getPassword() {
        return customer.getPasswordHash();
    }

    /**
     * Retourne l'email du customer comme username.
     */
    @Override
    public String getUsername() {
        return customer.getEmail();
    }

    /**
     * Compte non expiré (par défaut true).
     * Pour implémenter l'expiration, ajouter un champ accountExpiryDate dans Customer.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Compte non verrouillé (par défaut true).
     * Pour implémenter le verrouillage, ajouter un champ accountLocked dans Customer.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Credentials non expirés (par défaut true).
     * Pour implémenter l'expiration du mot de passe, ajouter passwordLastChangedDate dans Customer.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Compte activé (par défaut true).
     * Pour implémenter la vérification email, ajouter un champ emailVerified dans Customer.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}