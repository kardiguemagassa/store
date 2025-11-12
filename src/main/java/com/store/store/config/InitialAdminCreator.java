package com.store.store.config;

import com.store.store.entity.Customer;
import com.store.store.entity.Role;
import com.store.store.enums.RoleType;
import com.store.store.repository.CustomerRepository;
import com.store.store.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Kardigué
 * @version 1.0
 * @since 2025-10-01
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InitialAdminCreator implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(String... args) {
        // Vérifier si on est en environnement de test
        if (isTestProfileActive()) {
            log.info("InitialAdminCreator désactivé pour les tests");
            return;
        }

        // Vérifier si l'initialisation est désactivée via les propriétés
        if (!isInitialDataEnabled()) {
            log.info("InitialAdminCreator désactivé via configuration");
            return;
        }

        try {
            createAdminIfNotExists();
        } catch (Exception e) {
            log.error("Erreur lors de la création de l'admin initial", e);
            // Ne pas propager l'exception pour éviter de bloquer le démarrage
        }
    }

    private boolean isTestProfileActive() {
        return environment.acceptsProfiles(org.springframework.core.env.Profiles.of("test"));
    }

    private boolean isInitialDataEnabled() {
        return environment.getProperty("store.initial-data.enabled", Boolean.class, true);
    }

    private void createAdminIfNotExists() {
        String adminEmail = "admin@store.com";

        // Vérifier si l'admin existe déjà
        if (customerRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin utilisateur existe déjà: {}", adminEmail);
            return;
        }

        // Créer les rôles s'ils n'existent pas
        Role userRole = getOrCreateRole(RoleType.ROLE_USER);
        Role adminRole = getOrCreateRole(RoleType.ROLE_ADMIN);

        // Créer l'utilisateur admin
        Customer admin = new Customer();
        admin.setName("Admin Principal");
        admin.setEmail(adminEmail);
        admin.setMobileNumber("+33600000000");
        admin.setPasswordHash(passwordEncoder.encode("AdminSecurePass123!"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(adminRole);
        admin.setRoles(roles);

        Customer savedAdmin = customerRepository.save(admin);
        log.info("Premier administrateur créé : {} (ID: {})", savedAdmin.getEmail(), savedAdmin.getCustomerId());
    }

    private Role getOrCreateRole(RoleType roleType) {
        return roleRepository.findByName(roleType)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(roleType);
                    Role savedRole = roleRepository.save(newRole);
                    log.info("Rôle créé: {}", roleType);
                    return savedRole;
                });
    }
}