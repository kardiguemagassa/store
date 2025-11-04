package com.store.store.config;

import com.store.store.entity.Customer;
import com.store.store.entity.Product;
import com.store.store.entity.Category;
import com.store.store.entity.Role;
import com.store.store.enums.RoleType;
import com.store.store.repository.CustomerRepository;
import com.store.store.repository.ProductRepository;
import com.store.store.repository.CategoryRepository;
import com.store.store.repository.RoleRepository;
import com.store.store.security.CustomerUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
//@Import(TestSecurityConfig.class)
@Transactional
@Slf4j
class AuditingDiagnosticTest {

    @Autowired
    private AuditorAware<String> auditorAware;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Category testCategory;
    private Role userRole;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        // Pré-créer les données de base
        userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleType.ROLE_USER);
                    role.setDisplayName("User");
                    role.setIsActive(true);
                    return roleRepository.save(role);
                });

        testCategory = categoryRepository.save(createTestCategory());
    }

    @Test
    void contextLoads() {
        log.info("=== VÉRIFICATION DU CONTEXTE SPRING ===");
        assertNotNull(auditorAware, "AuditorAware doit être configuré");
        assertNotNull(customerRepository, "CustomerRepository doit être injecté");
        assertNotNull(productRepository, "ProductRepository doit être injecté");
        assertNotNull(categoryRepository, "CategoryRepository doit être injecté");
        log.info("✅ Contexte Spring chargé avec succès");
    }

    @Test
    void verifyCorrectAuditorAwareImplementation() {
        log.info("=== VÉRIFICATION DE L'IMPLÉMENTATION AUDITORAWARE ===");

        log.info("Type de auditorAware: {}", auditorAware.getClass().getName());
        log.info("Est-ce AuditorAwareImpl? {}", (auditorAware instanceof AuditorAwareImpl));

        assertTrue(auditorAware instanceof AuditorAwareImpl,
                "L'implémentation injectée doit être AuditorAwareImpl");

        log.info("✅ Bonne implémentation AuditorAware détectée");
    }

    @Test
    void testAuditingWithAuthenticatedUser() {
        log.info("=== TEST AUDITING AVEC AUTHENTIFICATION ===");

        // 1. Simuler un utilisateur authentifié
        setupAuthenticatedUser();

        // 2. Vérifier l'auditeur courant AVANT la sauvegarde
        Optional<String> currentAuditor = auditorAware.getCurrentAuditor();
        assertTrue(currentAuditor.isPresent(), "Auditeur courant doit être présent");
        assertEquals("magassakara@gmail.com", currentAuditor.get());
        log.info("👤 Utilisateur authentifié simulé: {}", currentAuditor.get());

        // 3. Créer et sauvegarder un produit
        Product product = createTestProduct(testCategory);
        Product savedProduct = productRepository.save(product);
        log.info("📦 Produit sauvegardé avec ID: {}", savedProduct.getId());

        // 4. Vérifier les champs d'audit
        assertAll("Vérification des champs d'audit",
                () -> assertNotNull(savedProduct.getCreatedAt(), "createdAt ne doit pas être null"),
                () -> assertNotNull(savedProduct.getUpdatedAt(), "updatedAt ne doit pas être null"),
                () -> assertEquals("magassakara@gmail.com", savedProduct.getCreatedBy(),
                        "createdBy doit correspondre à l'utilisateur authentifié"),
                () -> assertEquals("magassakara@gmail.com", savedProduct.getUpdatedBy(),
                        "updatedBy doit correspondre à l'utilisateur authentifié")
        );

        log.info("✅ Test d'audit réussi - CreatedBy: {}, UpdatedBy: {}",
                savedProduct.getCreatedBy(), savedProduct.getUpdatedBy());
    }

    @Test
    void testAuditingWithoutAuthentication() {
        log.info("=== TEST AUDITING SANS AUTHENTIFICATION ===");

        // S'assurer qu'aucune authentification n'est présente
        SecurityContextHolder.clearContext();

        // Vérifier que l'auditeur retourne "system"
        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertTrue(auditor.isPresent());
        assertEquals("system", auditor.get());
        log.info("🔧 Auditeur système détecté: {}", auditor.get());

        Product product = createTestProduct(testCategory);
        Product savedProduct = productRepository.save(product);

        // Vérifier que les champs sont remplis avec "system"
        assertAll("Vérification des champs d'audit système",
                () -> assertNotNull(savedProduct.getCreatedAt()),
                () -> assertNotNull(savedProduct.getUpdatedAt()),
                () -> assertEquals("system", savedProduct.getCreatedBy()),
                () -> assertEquals("system", savedProduct.getUpdatedBy())
        );

        log.info("✅ Auditing fonctionne correctement sans authentification");
    }

    /*@Test
    void testAuditorAwareDirectly() {
        log.info("=== TEST DIRECT DE AUDITORAWARE ===");

        // Vérification préalable
        assertTrue(auditorAware instanceof AuditorAwareImpl,
                "Mauvaise implémentation AuditorAware injectée");

        // Test 1: Sans authentification
        SecurityContextHolder.clearContext();
        Optional<String> auditor1 = auditorAware.getCurrentAuditor();
        assertTrue(auditor1.isPresent());
        assertEquals("system", auditor1.get());
        log.info("🔧 Sans auth: {}", auditor1.get());

        // Test 2: Avec authentification via UserDetails
        setupAuthenticatedUser();
        Optional<String> auditor2 = auditorAware.getCurrentAuditor();
        assertTrue(auditor2.isPresent());
        assertEquals("magassakara@gmail.com", auditor2.get());
        log.info("👤 Avec auth UserDetails: {}", auditor2.get());

        // Test 3: Avec principal String - MAINTENANT ça devrait marcher
        SecurityContextHolder.clearContext();
        Authentication auth = new UsernamePasswordAuthenticationToken("admin-user", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> auditor3 = auditorAware.getCurrentAuditor();
        assertTrue(auditor3.isPresent());
        assertEquals("admin-user", auditor3.get(),
                "AuditorAwareImpl doit gérer les principaux String");
        log.info("📝 Avec principal String: {}", auditor3.get());

        log.info("✅ Tous les scénarios AuditorAware fonctionnent");
    }*/

    private void setupAuthenticatedUser() {
        try {
            // Créer un customer avec des rôles valides
            Customer customer = new Customer();
            customer.setEmail("magassakara@gmail.com");
            customer.setPasswordHash("encodedPassword");
            customer.setName("Kara Magassa");
            customer.setRoles(Set.of(userRole));

            CustomerUserDetails userDetails = new CustomerUserDetails(customer);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("🔐 Authentification configurée pour: {}", customer.getEmail());
        } catch (Exception e) {
            log.error("❌ Erreur lors de la configuration de l'authentification", e);
            throw new RuntimeException("Échec de configuration de l'authentification", e);
        }
    }

    private Category createTestCategory() {
        Category category = new Category();
        category.setName("Catégorie Test Audit");
        category.setDescription("Description pour test d'auditing");
        category.setIcon("http://example.com/audit-icon.jpg");
        category.setIsActive(true);
        category.setCode("AUDIT_CAT");
        category.setDisplayOrder(1);
        return category;
    }

    private Product createTestProduct(Category category) {
        Product product = new Product();
        product.setName("Produit Test Audit");
        product.setDescription("Description pour test d'auditing");
        product.setPrice(BigDecimal.valueOf(29.99));
        product.setPopularity(5);
        product.setStockQuantity(50);
        product.setCategory(category);
        product.setImageUrl("http://example.com/audit-product.jpg");
        product.setIsActive(true);
        return product;
    }
}