package com.store.store.entity;

import com.store.store.enums.RoleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 100)
    @NotNull
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Size(min = 7, max = 20, message = "Le numéro de téléphone doit contenir entre 7 et 20 caractères")
    @Pattern(
            regexp = "^\\+?[0-9]{7,15}$",  //  optionnel, puis 7-15 chiffres
            message = "Format de numéro de téléphone invalide"
    )
    @Column(name = "mobile_number", nullable = false, unique = true, length = 20)
    private String mobileNumber;

    @Size(max = 500)
    @NotNull
    @Column(name = "password_hash", nullable = false, length = 500)
    private String passwordHash;

    @OneToOne(mappedBy = "customer",cascade = CascadeType.ALL)
    private Address address;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "customer_roles",
            joinColumns = @JoinColumn(name = "customer_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new LinkedHashSet<>();

    public boolean hasRole(RoleType roleType) {
        return roles.stream().anyMatch(role -> role.getName() == roleType);
    }

    public boolean isAdmin() {
        return hasRole(RoleType.ROLE_ADMIN);
    }

    public boolean isUser() {
        return hasRole(RoleType.ROLE_USER);
    }

    public boolean isManager() {
        return hasRole(RoleType.ROLE_MANAGER);
    }

    public boolean isEmployee() {
        return hasRole(RoleType.ROLE_EMPLOYEE);
    }

    public Set<String> getRoleNames() {
        return roles.stream().map(role -> role.getName().name()).collect(Collectors.toSet());
    }

    // Méthode utile pour la hiérarchie
    public boolean hasMinimumRole(RoleType minimumRole) {
        return roles.stream().anyMatch(role -> role.getName().getLevel() >= minimumRole.getLevel());
    }

}