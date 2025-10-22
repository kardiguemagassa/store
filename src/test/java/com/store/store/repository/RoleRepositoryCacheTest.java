package com.store.store.repository;

import com.store.store.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;


@SpringBootTest
@ActiveProfiles("test")
class RoleRepositoryCacheTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("findByName - Devrait mettre en cache les rôles")
    void shouldCacheRolesByName() {
        // Given
        Role role = new Role();
        role.setName("ROLE_CACHED");
        role.setCreatedBy("system");
        role.setCreatedAt(Instant.now());
        roleRepository.save(role);

        // When - Premier appel (CACHE MISS)
        Optional<Role> firstCall = roleRepository.findByName("ROLE_CACHED");

        // Then - Vérifier le cache
        Cache rolesCache = cacheManager.getCache("roles");
        assertThat(rolesCache).isNotNull();
        assertThat(rolesCache.get("ROLE_CACHED")).isNotNull();

        // When - Deuxième appel (CACHE HIT)
        Optional<Role> secondCall = roleRepository.findByName("ROLE_CACHED");

        // Then
        assertThat(firstCall).isPresent();
        assertThat(secondCall).isPresent();
        assertThat(firstCall.get()).isEqualTo(secondCall.get());
    }
}