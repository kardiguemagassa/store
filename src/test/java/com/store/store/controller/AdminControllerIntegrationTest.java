package com.store.store.controller;

import com.store.store.constants.ApplicationConstants;
import com.store.store.entity.Contact;
import com.store.store.entity.Customer;
import com.store.store.entity.Order;
import com.store.store.entity.Role;
import com.store.store.repository.ContactRepository;
import com.store.store.repository.CustomerRepository;
import com.store.store.repository.OrderRepository;
import com.store.store.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration du AdminController avec vraie base de données et sécurité complète.
 * Ces tests vérifient l'intégration complète : Controller -> Service -> Repository -> DB
 * Plus lents mais testent les scénarios réels end-to-end avec CSRF
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Slf4j
@DisplayName("Tests d'Intégration - AdminController")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Order testOrder;
    private Contact testContact;

    @BeforeEach
    void setUp() {
        // Nettoyer la base pour garantir un état initial propre
        cleanDatabase();

        // Créer de vraies entités
        testOrder = createRealOrder();
        testContact = createRealContact();

        log.info("Données de test créées : Order #{}, Contact #{}",
                testOrder.getOrderId(), testContact.getContactId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("E2E : Devrait récupérer les vraies commandes en attente depuis la DB")
    void shouldGetRealPendingOrdersFromDatabase() throws Exception {
        // Given - Une commande en attente créée dans le setUp

        // When & Then
        mockMvc.perform(get("/api/v1/admin/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderId", is(testOrder.getOrderId().intValue())))
                .andExpect(jsonPath("$[0].status", is(ApplicationConstants.ORDER_STATUS_CREATED)));

        log.info("Test E2E réussi : vraies données récupérées depuis la DB");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("E2E : Devrait confirmer une vraie commande et persister en DB")
    void shouldConfirmRealOrderAndPersistToDatabase() throws Exception {
        // Given - Une commande en attente

        // When & Then
        mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", testOrder.getOrderId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is("200")));

        // Vérification en base de données
        Order updatedOrder = orderRepository.findById(testOrder.getOrderId())
                .orElseThrow(() -> new AssertionError("La commande devrait exister en base"));
        assertEquals(ApplicationConstants.ORDER_STATUS_CONFIRMED, updatedOrder.getOrderStatus());

        log.info("Commande #{} confirmée et persistée : {}",
                updatedOrder.getOrderId(), updatedOrder.getOrderStatus());
    }

    //POUR SCÉNARIOS D'ERREUR
    @Nested
    @DisplayName("Tests d'erreurs E2E")
    class ErrorScenariosTest {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("E2E : Devrait gérer ResourceNotFoundException")
        void shouldHandleResourceNotFoundException() throws Exception {
            // Given - ID qui n'existe pas
            Long nonExistentId = 999999L;

            // When & Then
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", nonExistentId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("Not Found"))
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value(containsString("introuvable")));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("E2E : Devrait gérer MethodArgumentTypeMismatchException")
        void shouldHandleTypeMismatchException() throws Exception {
            // Given - ID invalide de type string

            // When & Then
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", "invalid-id")
                            .with(csrf())
                            .characterEncoding("UTF-8")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(result -> {
                        // Debug logging
                        log.debug("Response: {}", result.getResponse().getContentAsString());
                    })
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"))
                    .andExpect(jsonPath("$.message",
                            anyOf(
                                    containsString("doit être de type"),
                                    containsString("doit"),
                                    containsString("type")
                            )));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("E2E : Devrait gérer ConstraintViolationException pour IDs invalides")
        void shouldHandleConstraintViolationException() throws Exception {
            // Given - ID négatif

            // When & Then
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", -1)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").exists())
                    .andExpect(jsonPath("$.errorCode").value("CONSTRAINT_VIOLATION"))
                    // CORRECTION : Vérifier le format complet du message
                    .andExpect(jsonPath("$.errors.orderId").value(containsString("doit être positif")));
        }
    }

    // MÉTHODES UTILITAIRES
    private void cleanDatabase() {
        // Ordre de suppression important pour éviter les violations de contraintes
        orderRepository.deleteAll();
        contactRepository.deleteAll();
        customerRepository.deleteAll();
        roleRepository.deleteAll();
    }

    private Order createRealOrder() {
        Customer customer = createCustomer();

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderStatus(ApplicationConstants.ORDER_STATUS_CREATED);
        order.setTotalPrice(BigDecimal.valueOf(175.00));
        order.setPaymentStatus("PENDING");
        order.setPaymentIntentId("test-payment-" + System.currentTimeMillis());
        order.setCreatedAt(Instant.now());

        return orderRepository.save(order);
    }

    private Contact createRealContact() {
        Contact contact = new Contact();
        contact.setName("Integration Test User");
        contact.setEmail("integration@test.com");
        contact.setMobileNumber("0612345678");
        contact.setMessage("Test message from integration test");
        contact.setStatus(ApplicationConstants.OPEN_MESSAGE);
        contact.setCreatedAt(Instant.now());

        return contactRepository.save(contact);
    }

    private Customer createCustomer() {
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_USER");
                    role.setCreatedBy("TEST");
                    return roleRepository.save(role);
                });

        Customer customer = new Customer();
        customer.setEmail("test-" + System.currentTimeMillis() + "@example.com");
        customer.setName("Test Customer");
        customer.setMobileNumber("0123456789");
        customer.setPasswordHash("$2a$10$testEncodedPassword");
        customer.setRoles(new HashSet<>(Set.of(userRole)));
        customer.setCreatedAt(Instant.now());

        return customerRepository.save(customer);
    }
}

/*
## Stratégie Recommandée en Production

### **Règle 70-20-10**

  70% - Tests Unitaires (@WebMvcTest, Mockito)
  ├─ Rapides, nombreux
  ├─ Testent la logique métier
  └─ Feedback immédiat

20% - Tests d'Intégration (@SpringBootTest)
        ├─ Testent les flux complets
  ├─ Vérifient la persistance
  └─ Valident les interactions réelles

10% - Tests E2E (Selenium, Cypress)
  ├─ Scénarios utilisateurs critiques
  ├─ Tests UI complets
  └─ Smoke tests production

src/test/java/
        ├── controller/
        │   ├── AdminControllerTest.java              ← 70% (Unitaire, rapide)
        │   └── AdminControllerIntegrationTest.java   ← 20% (Intégration, critique)
        ├── service/
        │   ├── OrderServiceTest.java
│   └── OrderServiceIntegrationTest.java
└── security/
        ├── StoreSecurityConfigUnitTest.java
    └── StoreSecurityConfigIntegrationTest.java

 */