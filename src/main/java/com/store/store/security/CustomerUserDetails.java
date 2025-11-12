package com.store.store.security;

import com.store.store.entity.Customer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enregistrement implémentant l'interface {@code UserDetails} et encapsulant une entité {@code Customer}.
 * Cet enregistrement permet d'adapter l'entité {@code Customer} à l'interface {@code UserDetails} de Spring Security,
 * fournissant les informations d'authentification et d'autorisation.
 *
 * @author Kardigué
 * @version 2.0
 * @since 2025-10-01
 */
public record CustomerUserDetails(Customer customer) implements UserDetails {

    /**
     * Constructeur de l'enregistrement CustomerUserDetails. Valide l'objet Customer fourni
     * et vérifie que ses champs email et mot de passe ne sont ni nuls ni vides.
     * @param customer l'objet Customer à encapsuler. Ne doit pas être nul.
     * L'adresse email du client ne doit pas être nulle ni vide.
     * Le hachage du mot de passe du client ne doit pas être nul ni vide.
     * @throws IllegalArgumentException si le client est nul, si l'adresse email est nulle ou vide,
     * ou si le hachage du mot de passe est nul ou vide.
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
     * Récupère la collection des autorisations accordées aux rôles du client.
     * Cette méthode transforme les rôles du client en une collection d'objets {@code GrantedAuthority}.
     * Chaque rôle est associé à un objet {@link SimpleGrantedAuthority}
     * à l'aide du nom du rôle.
     * @return une collection d'objets {@code GrantedAuthority} représentant les autorisations du client,
     * ou une collection vide si le client ne possède aucun rôle ou si ses rôles sont nuls.
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
     * Récupère le hachage du mot de passe du client associé.
     * @return le hachage du mot de passe du client sous forme de chaîne de caractères.
     */
    @Override
    public String getPassword() {
        return customer.getPasswordHash();
    }

    /**
     * Récupère le nom d'utilisateur associé au client.
     * Cette méthode renvoie l'adresse e-mail du client, qui sert de nom d'utilisateur.
     * @return l'adresse e-mail du client sous forme de chaîne de caractères, représentant le nom d'utilisateur.
     */
    @Override
    public String getUsername() {
        return customer.getEmail();
    }

    /**
     * Indique si le compte du client est actif.
     * Cette méthode renvoie la valeur constante {@code true}, ce qui signifie
     * que le compte n'est jamais considéré comme expiré.
     * @return {@code true} si le compte est actif; sinon, {@code false}.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Vérifie si le compte du client n'est pas verrouillé.
     * Cette méthode renvoie toujours {@code true}, ce qui signifie que le compte
     * n'est jamais considéré comme verrouillé.
     *
     * @return {@code true} si le compte n'est pas verrouillé; sinon, {@code false}.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indique si les identifiants du client sont valides.
     * Cette méthode renvoie toujours {@code true}, indiquant que les identifiants
     * ne sont jamais considérés comme expirés.
     * @return {@code true} si les identifiants sont valides; sinon, {@code false}.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indique si le compte du client est activé.
     * Cette méthode renvoie toujours {@code true}, ce qui signifie que le compte
     * est activé de façon permanente et ne peut pas être désactivé.
     * @return {@code true} si le compte est activé; sinon, {@code false}.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}