package com.store.store.repository;

import com.store.store.entity.Role;
import com.store.store.enums.RoleType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    @Cacheable("roles")
        // ROLE_USER -> CACHE MISS -> DB call -> Cache Store (ROLE_USER -> Role record) -> Customer 1
        // ROLE_USER -> CACHE HIT -> Customer 2
        // ROLE_ADMIN -> CACHE MISS -> DB call -> Cache Store (ROLE_ADMIN -> Role record) -> Customer X
    Optional<Role> findByName(RoleType name);  // ✅ CORRIGÉ : RoleType au lieu de String

    List<Role> findAllByIsActiveTrue();
    boolean existsByName(RoleType name);
}
