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
        // vider la base pour garantir un état initial propre
        orderRepository.deleteAll();
        contactRepository.deleteAll();
        customerRepository.deleteAll();
        roleRepository.deleteAll();

        // crée de vraies entités (Order, Contact) via des méthodes d'aide (createRealOrder(), createRealContact())
        testOrder = createRealOrder();
        testContact = createRealContact();

        log.info("Données de test créées : Order #{}, Contact #{}",
                testOrder.getOrderId(), testContact.getContactId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("E2E : Devrait récupérer les vraies commandes en attente depuis la DB")
    void shouldGetRealPendingOrdersFromDatabase() throws Exception {
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
        // with(csrf()) pour les requêtes PATCH
        mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", testOrder.getOrderId())
                        .with(csrf())  // Token CSRF ajouté
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is("200")));

        Order updatedOrder = orderRepository.findById(testOrder.getOrderId()).orElseThrow();
        assertEquals(ApplicationConstants.ORDER_STATUS_CONFIRMED, updatedOrder.getOrderStatus());

        log.info("Commande #{} confirmée et persistée : {}",
                updatedOrder.getOrderId(), updatedOrder.getOrderStatus());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("E2E : Devrait annuler une vraie commande et persister en DB")
    void shouldCancelRealOrderAndPersistToDatabase() throws Exception {
        // with(csrf())
        mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/cancel", testOrder.getOrderId())
                        .with(csrf())  // token CSRF ajoutét
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        Order cancelledOrder = orderRepository.findById(testOrder.getOrderId()).orElseThrow();
        assertEquals(ApplicationConstants.ORDER_STATUS_CANCELLED, cancelledOrder.getOrderStatus());

        log.info("Commande #{} annulée et persistée", cancelledOrder.getOrderId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("E2E : Devrait récupérer les vrais messages ouverts depuis la DB")
    void shouldGetRealOpenMessagesFromDatabase() throws Exception {
        mockMvc.perform(get("/api/v1/admin/messages")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                // Vérifie que le JSON racine ($) est un tableau contenant exactement 1 élément
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].contactId", is(testContact.getContactId().intValue())))
                .andExpect(jsonPath("$[0].status", is(ApplicationConstants.OPEN_MESSAGE)));

        log.info("Messages réels récupérés depuis la DB");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("E2E : Devrait fermer un vrai message et persister en DB")
    void shouldCloseRealMessageAndPersistToDatabase() throws Exception {
        // Ajouter .with(csrf())
        mockMvc.perform(patch("/api/v1/admin/messages/{contactId}/close", testContact.getContactId())
                        .with(csrf())  //Token CSRF ajouté
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        Contact closedContact = contactRepository.findById(testContact.getContactId()).orElseThrow();
        assertEquals(ApplicationConstants.CLOSED_MESSAGE, closedContact.getStatus());

        log.info("Message #{} fermé et persisté", closedContact.getContactId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("E2E : Scénario complet - Créer, confirmer, vérifier")
    void completeEndToEndScenario() throws Exception {
        // 1. Vérifier que la commande existe
        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // 2. Confirmer la commande - Ajouter .with(csrf())
        mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", testOrder.getOrderId())
                        .with(csrf()))  // Token CSRF ajouté
                .andExpect(status().isOk());

        // 3. Vérifier que la liste est vide
        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        log.info("Scénario E2E complet réussi");
    }

    // NESTED CLASSES
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
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", "invalid-id")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("paramètre")));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("E2E : Devrait gérer ConstraintViolationException pour IDs invalides")
        void shouldHandleConstraintViolationException() throws Exception {
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", -1)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").exists())
                    .andExpect(jsonPath("$.errors.orderId").value("L'ID de commande doit être positif"));
        }
    }

    @Nested
    @DisplayName("Tests de sécurité E2E")
    class SecurityScenariosTest {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("E2E : Devrait refuser l'accès aux non-admins")
        void shouldDenyAccessForNonAdminUsers() throws Exception {
            mockMvc.perform(get("/api/v1/admin/orders"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("E2E : Devrait refuser l'accès aux non authentifiés")
        void shouldDenyAccessForUnauthenticatedUsers() throws Exception {
            mockMvc.perform(get("/api/v1/admin/orders"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // HELPERS
    private Order createRealOrder() {
        Customer customer = createCustomer();

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderStatus(ApplicationConstants.ORDER_STATUS_CREATED);
        order.setTotalPrice(BigDecimal.valueOf(175.00));
        order.setPaymentStatus("PENDING");
        order.setPaymentId("test-payment-" + System.currentTimeMillis());

        return orderRepository.save(order);
    }

    private Contact createRealContact() {
        Contact contact = new Contact();
        contact.setName("Integration Test User");
        contact.setEmail("integration@test.com");
        contact.setMobileNumber("0612345678");
        contact.setMessage("Test message");
        contact.setStatus(ApplicationConstants.OPEN_MESSAGE);

        return contactRepository.save(contact);
    }

    private Customer createCustomer() {
        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        userRole.setCreatedBy("TEST");
        roleRepository.save(userRole);

        Customer customer = new Customer();
        customer.setEmail("test-" + System.currentTimeMillis() + "@example.com");
        customer.setName("Test Customer");
        customer.setMobileNumber("0123456789");
        customer.setPasswordHash("$2a$10$test");
        customer.setRoles(new HashSet<>(Set.of(userRole)));

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