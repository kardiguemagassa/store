package com.store.store.repository;

import com.store.store.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests d'intégration pour RoleRepository
 * Teste les opérations CRUD, la requête personnalisée et le cache
 */
/*@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests du RoleRepository")
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        roleRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    // ==================== TESTS CRUD DE BASE ====================

    @Test
    @DisplayName("Devrait sauvegarder un rôle avec succès")
    void shouldSaveRole() {
        // Given
        Role role = createRole(null, "ROLE_USER");

        // When
        Role savedRole = roleRepository.save(role);

        // Then
        assertThat(savedRole).isNotNull();
        assertThat(savedRole.getRoleId()).isNotNull();
        assertThat(savedRole.getName()).isEqualTo("ROLE_USER");
        assertThat(savedRole.getCreatedAt()).isNotNull();
        assertThat(savedRole.getCreatedBy()).isEqualTo("system");
    }

    @Test
    @DisplayName("Devrait trouver un rôle par son ID")
    void shouldFindRoleById() {
        // Given
        Role role = createRole(null, "ROLE_ADMIN");
        Role savedRole = entityManager.persistAndFlush(role);
        Long roleId = savedRole.getRoleId();

        // When
        Optional<Role> foundRole = roleRepository.findById(roleId);

        // Then
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getName()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Devrait retourner Optional.empty() pour un ID inexistant")
    void shouldReturnEmptyForNonExistentId() {
        // When
        Optional<Role> foundRole = roleRepository.findById(999L);

        // Then
        assertThat(foundRole).isEmpty();
    }

    @Test
    @DisplayName("Devrait récupérer tous les rôles")
    void shouldFindAllRoles() {
        // Given
        Role role1 = createRole(null, "ROLE_USER");
        Role role2 = createRole(null, "ROLE_ADMIN");
        Role role3 = createRole(null, "ROLE_MANAGER");

        entityManager.persist(role1);
        entityManager.persist(role2);
        entityManager.persist(role3);
        entityManager.flush();

        // When
        List<Role> roles = roleRepository.findAll();

        // Then
        assertThat(roles).hasSize(3);
        assertThat(roles).extracting(Role::getName)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_MANAGER");
    }

    @Test
    @DisplayName("Devrait mettre à jour un rôle existant")
    void shouldUpdateExistingRole() {
        // Given
        Role role = createRole(null, "ROLE_USER");
        Role savedRole = entityManager.persistAndFlush(role);
        Long roleId = savedRole.getRoleId();

        // When
        savedRole.setName("ROLE_POWER_USER");
        savedRole.setUpdatedAt(Instant.now());
        savedRole.setUpdatedBy("admin");
        roleRepository.save(savedRole);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Role> updatedRole = roleRepository.findById(roleId);
        assertThat(updatedRole).isPresent();
        assertThat(updatedRole.get().getName()).isEqualTo("ROLE_POWER_USER");
        assertThat(updatedRole.get().getUpdatedAt()).isNotNull();
        assertThat(updatedRole.get().getUpdatedBy()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Devrait supprimer un rôle par son ID")
    void shouldDeleteRoleById() {
        // Given
        Role role = createRole(null, "ROLE_TEMPORARY");
        Role savedRole = entityManager.persistAndFlush(role);
        Long roleId = savedRole.getRoleId();

        // When
        roleRepository.deleteById(roleId);
        entityManager.flush();

        // Then
        Optional<Role> deletedRole = roleRepository.findById(roleId);
        assertThat(deletedRole).isEmpty();
    }

    @Test
    @DisplayName("Devrait compter le nombre total de rôles")
    void shouldCountAllRoles() {
        // Given
        Role role1 = createRole(null, "ROLE_USER");
        Role role2 = createRole(null, "ROLE_ADMIN");
        Role role3 = createRole(null, "ROLE_GUEST");

        entityManager.persist(role1);
        entityManager.persist(role2);
        entityManager.persist(role3);
        entityManager.flush();

        // When
        long count = roleRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Devrait vérifier l'existence d'un rôle par ID")
    void shouldCheckRoleExistence() {
        // Given
        Role role = createRole(null, "ROLE_TESTER");
        Role savedRole = entityManager.persistAndFlush(role);
        Long roleId = savedRole.getRoleId();

        // When
        boolean exists = roleRepository.existsById(roleId);
        boolean notExists = roleRepository.existsById(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Devrait supprimer tous les rôles")
    void shouldDeleteAllRoles() {
        // Given
        Role role1 = createRole(null, "ROLE_DELETE1");
        Role role2 = createRole(null, "ROLE_DELETE2");

        entityManager.persist(role1);
        entityManager.persist(role2);
        entityManager.flush();

        // When
        roleRepository.deleteAll();
        entityManager.flush();

        // Then
        assertThat(roleRepository.count()).isZero();
        assertThat(roleRepository.findAll()).isEmpty();
    }

    // ==================== TESTS REQUÊTE PERSONNALISÉE ====================

    @Test
    @DisplayName("findByName - Devrait trouver un rôle par son nom")
    void shouldFindRoleByName() {
        // Given
        Role role = createRole(null, "ROLE_MODERATOR");
        entityManager.persistAndFlush(role);

        // When
        Optional<Role> foundRole = roleRepository.findByName("ROLE_MODERATOR");

        // Then
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getName()).isEqualTo("ROLE_MODERATOR");
    }

    @Test
    @DisplayName("findByName - Devrait retourner empty pour un nom inexistant")
    void shouldReturnEmptyForNonExistentName() {
        // When
        Optional<Role> foundRole = roleRepository.findByName("ROLE_NONEXISTENT");

        // Then
        assertThat(foundRole).isEmpty();
    }

    @Test
    @DisplayName("findByName - Devrait être sensible à la casse")
    void shouldFindRoleByNameCaseSensitive() {
        // Given
        Role role = createRole(null, "ROLE_USER");
        entityManager.persistAndFlush(role);

        // When
        Optional<Role> foundUpperCase = roleRepository.findByName("ROLE_USER");
        Optional<Role> foundLowerCase = roleRepository.findByName("role_user");

        // Then
        assertThat(foundUpperCase).isPresent();
        assertThat(foundLowerCase).isEmpty(); // Case-sensitive
    }

    @Test
    @DisplayName("findByName - Devrait trouver les rôles standards du système")
    void shouldFindStandardSystemRoles() {
        // Given - Créer les rôles standards
        Role userRole = createRole(null, "ROLE_USER");
        Role adminRole = createRole(null, "ROLE_ADMIN");
        Role opsRole = createRole(null, "ROLE_OPS_ENG");
        Role qaRole = createRole(null, "ROLE_QA_ENG");

        entityManager.persist(userRole);
        entityManager.persist(adminRole);
        entityManager.persist(opsRole);
        entityManager.persist(qaRole);
        entityManager.flush();

        // When
        Optional<Role> foundUser = roleRepository.findByName("ROLE_USER");
        Optional<Role> foundAdmin = roleRepository.findByName("ROLE_ADMIN");
        Optional<Role> foundOps = roleRepository.findByName("ROLE_OPS_ENG");
        Optional<Role> foundQa = roleRepository.findByName("ROLE_QA_ENG");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundAdmin).isPresent();
        assertThat(foundOps).isPresent();
        assertThat(foundQa).isPresent();
    }

    // ==================== TESTS DE CONTRAINTES ====================

    @Test
    @DisplayName("Devrait lever une exception pour un nom de rôle dupliqué")
    void shouldThrowExceptionForDuplicateRoleName() {
        // Given
        Role role1 = createRole(null, "ROLE_DUPLICATE");
        roleRepository.saveAndFlush(role1);

        Role role2 = createRole(null, "ROLE_DUPLICATE");

        // When/Then
        assertThatThrownBy(() -> roleRepository.saveAndFlush(role2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Devrait accepter deux rôles avec des noms différents")
    void shouldAcceptTwoRolesWithDifferentNames() {
        // Given
        Role role1 = createRole(null, "ROLE_ALPHA");
        Role role2 = createRole(null, "ROLE_BETA");

        // When
        roleRepository.save(role1);
        roleRepository.save(role2);
        entityManager.flush();

        // Then
        assertThat(roleRepository.count()).isEqualTo(2);
    }

    // ==================== TESTS DE VALIDATION ====================

    @Test
    @DisplayName("Devrait persister toutes les propriétés d'un rôle")
    void shouldPersistAllRoleProperties() {
        // Given
        Role role = createRole(null, "ROLE_COMPLETE");
        role.setCreatedBy("test-admin");

        // When
        Role savedRole = roleRepository.save(role);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Role> retrievedRole = roleRepository.findById(savedRole.getRoleId());
        assertThat(retrievedRole).isPresent();
        Role found = retrievedRole.get();

        assertThat(found.getName()).isEqualTo("ROLE_COMPLETE");
        assertThat(found.getCreatedBy()).isEqualTo("test-admin");
        assertThat(found.getCreatedAt()).isNotNull();
        // Ne pas vérifier updatedBy/updatedAt lors de la création
        assertThat(found.getUpdatedBy()).isNull();
        assertThat(found.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("Devrait gérer correctement les valeurs null pour les champs optionnels")
    void shouldHandleNullOptionalFields() {
        // Given
        Role role = createRole(null, "ROLE_NULLABLE");
        role.setUpdatedAt(null);
        role.setUpdatedBy(null);

        // When
        Role savedRole = roleRepository.save(role);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Role> retrievedRole = roleRepository.findById(savedRole.getRoleId());
        assertThat(retrievedRole).isPresent();
        assertThat(retrievedRole.get().getUpdatedAt()).isNull();
        assertThat(retrievedRole.get().getUpdatedBy()).isNull();
    }

    @Test
    @DisplayName("Devrait conserver les timestamps lors de la mise à jour")
    void shouldPreserveTimestampsOnUpdate() {
        // Given
        Role role = createRole(null, "ROLE_TIMESTAMP_TEST");
        Role savedRole = entityManager.persistAndFlush(role);
        Instant originalCreatedAt = savedRole.getCreatedAt();
        Long roleId = savedRole.getRoleId();

        // Wait a bit to ensure timestamps would differ
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        savedRole.setName("ROLE_TIMESTAMP_TEST_UPDATED");
        savedRole.setUpdatedAt(Instant.now());
        savedRole.setUpdatedBy("updater");
        roleRepository.save(savedRole);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Role> updatedRole = roleRepository.findById(roleId);
        assertThat(updatedRole).isPresent();
        assertThat(updatedRole.get().getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(updatedRole.get().getUpdatedAt()).isNotNull();
        assertThat(updatedRole.get().getUpdatedAt()).isAfter(originalCreatedAt);
    }

    // ==================== MÉTHODES UTILITAIRES ====================

 */

    /**
     * Crée un objet Role pour les tests
     */
    /*private Role createRole(Long id, String name) {
        Role role = new Role();
        role.setRoleId(id);
        role.setName(name);
        role.setCreatedBy("system");
        role.setCreatedAt(Instant.now());
        return role;
    }


}

     */