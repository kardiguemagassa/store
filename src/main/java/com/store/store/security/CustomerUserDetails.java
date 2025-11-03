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
 * A record that implements the {@code UserDetails} interface, wrapping a {@code Customer} entity.
 * This record is used to adapt the {@code Customer} entity to the Spring Security {@code UserDetails} interface,
 * providing authentication and authorization details.
 *
 * @author Kardigué
 * @version 2.0
 * @since 2025-10-01
 */
public record CustomerUserDetails(Customer customer) implements UserDetails {

    /**
     * Constructor for the CustomerUserDetails record. Validates the provided Customer object
     * and ensures that its email and password fields are non-null and not blank.
     *
     * @param customer the Customer object to encapsulate. Must not be null.
     *                 The email of the customer must not be null or blank.
     *                 The password hash of the customer must not be null or blank.
     * @throws IllegalArgumentException if the customer is null, the email is null or blank,
     *                                  or the password hash is null or blank.
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
     * Retrieves the collection of granted authorities associated with the customer's roles.
     * This method transforms the roles of the customer into a collection of {@code GrantedAuthority} objects.
     * Each role is mapped to a {@link SimpleGrantedAuthority}
     * using the role's name.
     *
     * @return a collection of {@code GrantedAuthority} objects representing the customer's authorities,
     *         or an empty collection if the customer has no roles or the roles are null.
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
     * Retrieves the password hash of the associated customer.
     *
     * @return the password hash of the customer as a String.
     */
    @Override
    public String getPassword() {
        return customer.getPasswordHash();
    }

    /**
     * Retrieves the username associated with the customer.
     * This method returns the email address of the customer, which serves as the username.
     *
     * @return the email address of the customer as a String, representing the username.
     */
    @Override
    public String getUsername() {
        return customer.getEmail();
    }

    /**
     * Indicates whether the customer's account is non-expired.
     * This method returns a constant value of {@code true}, implying
     * that the account is never considered expired.
     *
     * @return {@code true} if the account is non-expired; otherwise, {@code false}.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Checks whether the customer's account is not locked.
     * This method always returns {@code true}, implying that the account
     * is never considered locked.
     *
     * @return {@code true} if the account is not locked; otherwise, {@code false}.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the customer's credentials are non-expired.
     * This method always returns {@code true}, indicating that the credentials
     * are never considered expired.
     *
     * @return {@code true} if the credentials are non-expired; otherwise, {@code false}.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the customer's account is enabled.
     * This method always returns {@code true}, implying that the account
     * is permanently enabled and cannot be disabled.
     *
     * @return {@code true} if the account is enabled; otherwise, {@code false}.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}