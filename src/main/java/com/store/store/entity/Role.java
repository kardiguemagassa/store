package com.store.store.entity;

import com.store.store.enums.RoleType;
import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true)
    private RoleType name;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_system")
    private Boolean isSystem = false;

    @ManyToMany(mappedBy = "roles")
    private Set<Customer> customers = new LinkedHashSet<>();

    // Méthodes utilitaires - COMPLÉTER
    public boolean isAdmin() {
        return name == RoleType.ROLE_ADMIN;
    }

    public boolean isUser() {
        return name == RoleType.ROLE_USER;
    }

    public boolean isManager() {  // AJOUTER
        return name == RoleType.ROLE_MANAGER;
    }

    public boolean isEmployee() {  // AJOUTER
        return name == RoleType.ROLE_EMPLOYEE;
    }

    public boolean isSystemRole() {
        return isSystem != null && isSystem;
    }
}