// ============================================================
// 3. TESTS D'INTÉGRATION - StoreSecurityConfigIntegrationTest.java
// ============================================================
/*package com.store.store.security;

import com.store.store.entity.Customer;
import com.store.store.entity.Role;
import com.store.store.repository.CustomerRepository;
import com.store.store.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration complets avec contexte Spring et base de données.
 * Testent l'interaction réelle entre tous les composants de sécurité.
 * Usage : Validation des scénarios complets
 */
/*
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Slf4j
class StoreSecurityConfigIntegrationTest {

    @Autowired
    private StoreSecurityConfig config;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    @DisplayName("Le contexte Spring devrait charger tous les beans de sécurité")
    void contextLoads() {
        assertNotNull(config, "StoreSecurityConfig devrait être chargé");
        assertNotNull(customerRepository, "CustomerRepository devrait être chargé");
        assertNotNull(roleRepository, "RoleRepository devrait être chargé");
        assertNotNull(passwordEncoder, "PasswordEncoder devrait être chargé");

        assertTrue(applicationContext.containsBean("storeSecurityConfig"));
        log.info("✅ Contexte Spring chargé avec tous les beans de sécurité");
    }

    @Test
    @DisplayName("PasswordEncoder devrait fonctionner avec des données persistées en base")
    void passwordEncoder_ShouldWorkWithRealDatabase() {
        // Arrange
        Role userRole = createAndSaveRole("ROLE_USER");
        Customer customer = createCustomer("test@example.com", "Test User");

        String rawPassword = "password123";
        customer.setPasswordHash(passwordEncoder.encode(rawPassword));
        customer.setRoles(new HashSet<>(Set.of(userRole)));
        customerRepository.save(customer);

        // Act
        Customer saved = customerRepository.findByEmail("test@example.com").orElseThrow();

        // Assert
        assertTrue(passwordEncoder.matches(rawPassword, saved.getPasswordHash()));
        assertFalse(passwordEncoder.matches("wrongPassword", saved.getPasswordHash()));
        log.info("✅ PasswordEncoder fonctionne avec la base de données");
    }

    @Test
    @DisplayName("L'infrastructure de sécurité complète devrait être fonctionnelle E2E")
    void securityInfrastructure_ShouldBeCompletelyFunctional() {
        // 1. Créer des rôles
        Role userRole = createAndSaveRole("ROLE_USER");
        Role adminRole = createAndSaveRole("ROLE_ADMIN");

        // 2. Créer un customer avec rôles et mot de passe encodé
        Customer customer = createCustomer("integration@example.com", "Integration User");
        customer.setPasswordHash(passwordEncoder.encode("testpass"));
        customer.setRoles(new HashSet<>(Set.of(userRole, adminRole)));
        customerRepository.save(customer);

        // 3. Vérifier la persistance complète
        Customer saved = customerRepository.findByEmail("integration@example.com").orElseThrow();

        assertEquals("integration@example.com", saved.getEmail());
        assertEquals(2, saved.getRoles().size());
        assertTrue(passwordEncoder.matches("testpass", saved.getPasswordHash()));

        log.info("✅ Infrastructure de sécurité E2E complètement fonctionnelle");
    }

    @Test
    @DisplayName("La configuration CORS devrait être accessible depuis le contexte Spring")
    void corsConfiguration_ShouldBeAccessibleFromContext() {
        var corsConfig = config.corsConfigurationSource();
        assertNotNull(corsConfig);

        var urlBasedSource = (org.springframework.web.cors.UrlBasedCorsConfigurationSource) corsConfig;
        var corsConf = urlBasedSource.getCorsConfigurations().get("/**");

        assertNotNull(corsConf);
        assertEquals(Boolean.TRUE, corsConf.getAllowCredentials());
        assertEquals(3600L, corsConf.getMaxAge());

        log.info("✅ Configuration CORS accessible depuis le contexte");
    }

    @Test
    @DisplayName("Le bean publicPaths devrait être accessible et contenir les chemins critiques")
    void publicPathsBean_ShouldBeAccessibleFromContext() {
        // Arrange & Act
        List<String> publicPaths = applicationContext.getBean("publicPaths", List.class);

        // Assert
        assertNotNull(publicPaths);
        assertFalse(publicPaths.isEmpty());

        assertTrue(publicPaths.stream().anyMatch(path -> path.contains("/api/v1/products")));
        assertTrue(publicPaths.stream().anyMatch(path -> path.contains("/api/v1/auth")));
        assertTrue(publicPaths.stream().anyMatch(path -> path.contains("/store/actuator/health")));

        log.info("✅ PublicPaths bean accessible avec {} chemins publics", publicPaths.size());
    }

    @Test
    @DisplayName("Le bean AuthenticationManager devrait être accessible")
    void authenticationManager_ShouldBeAccessibleAsBean() {
        // Arrange & Act
        assertTrue(applicationContext.containsBean("authenticationManager"));

        var authManager = applicationContext.getBean("authenticationManager", AuthenticationManager.class);

        // Assert
        assertNotNull(authManager);
        assertInstanceOf(org.springframework.security.authentication.ProviderManager.class, authManager);

        log.info("✅ AuthenticationManager configuré et accessible comme bean");
    }

    // Méthodes utilitaires
    private Role createAndSaveRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        role.setCreatedBy("TEST");
        return roleRepository.save(role);
    }

    private Customer createCustomer(String email, String name) {
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setName(name);
        customer.setMobileNumber("0123456789");
        customer.setPasswordHash("temp"); // Sera remplacé
        return customer;
    }
}

 */
