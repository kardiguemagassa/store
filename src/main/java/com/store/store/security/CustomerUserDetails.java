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

public record CustomerUserDetails(Customer customer) implements UserDetails {

    // Constructeur compact avec validations
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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Optional.ofNullable(customer.getRoles())
                .orElse(Set.of())
                .stream()
                .filter(Objects::nonNull)
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return customer.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return customer.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}