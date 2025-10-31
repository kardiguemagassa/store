package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.config.TestSecurityConfig;
import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.ContactResponseDto;
import com.store.store.dto.OrderResponseDto;
import com.store.store.exception.*;
import com.store.store.repository.CustomerRepository;
import com.store.store.service.IContactService;
import com.store.store.service.IOrderService;
import com.store.store.service.IRoleAssignmentService;
import com.store.store.util.TestDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@Import(TestSecurityConfig.class)
@Slf4j
@ActiveProfiles("test")
@DisplayName("Tests du AdminController")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IOrderService orderService;

    @MockitoBean
    private IContactService contactService;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private IRoleAssignmentService roleAssignmentService;

    @MockitoBean
    private ExceptionFactory exceptionFactory;

    @BeforeEach
    void setUp() {
        // Données de test
        List<OrderResponseDto> mockOrders = Arrays.asList(
                TestDataBuilder.createOrderResponseDto(1L),
                TestDataBuilder.createOrderResponseDto(2L)
        );

        List<ContactResponseDto> mockMessages = Arrays.asList(
                TestDataBuilder.createContactResponseDto(1L),
                TestDataBuilder.createContactResponseDto(2L)
        );

        // Configuration des mocks par défaut
        when(orderService.getAllPendingOrders()).thenReturn(mockOrders);
        when(contactService.getAllOpenMessages()).thenReturn(mockMessages);
        doNothing().when(orderService).updateOrderStatus(anyLong(), anyString());
        doNothing().when(contactService).updateMessageStatus(anyLong(), anyString());

        // CORRECTION : Configuration complète des exceptions
        when(exceptionFactory.orderNotFound(anyLong()))
                .thenAnswer(invocation -> {
                    Long orderId = invocation.getArgument(0);
                    return new OrderNotFoundException(orderId);
                });

        when(exceptionFactory.contactNotFound(anyLong()))
                .thenAnswer(invocation -> {
                    Long contactId = invocation.getArgument(0);
                    return new ContactNotFoundException(contactId);
                });

        when(exceptionFactory.resourceNotFound(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String resource = invocation.getArgument(0);
                    String field = invocation.getArgument(1);
                    String value = invocation.getArgument(2);
                    return new ResourceNotFoundException(resource, field, value);
                });

        when(exceptionFactory.businessError(anyString()))
                .thenAnswer(invocation -> {
                    String message = invocation.getArgument(0);
                    return new BusinessException(message);
                });
    }

    //COMMANDES EN ATTENTE
    @Nested
    @DisplayName("GET /api/v1/admin/orders - Récupérer les commandes en attente")
    class GetAllPendingOrdersTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner la liste des commandes en attente")
        void shouldReturnAllPendingOrders() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].orderId", is(1)))
                    .andExpect(jsonPath("$[1].orderId", is(2)));

            verify(orderService).getAllPendingOrders();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner une liste vide quand il n'y a pas de commandes")
        void shouldReturnEmptyListWhenNoPendingOrders() throws Exception {
            // Arrange
            when(orderService.getAllPendingOrders()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", empty()));

            verify(orderService).getAllPendingOrders();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Devrait refuser l'accès aux utilisateurs non-admin")
        void shouldDenyAccessForNonAdminUsers() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());

            verify(orderService, never()).getAllPendingOrders();
        }
    }

    // CONFIRMATION COMMANDE
    @Nested
    @DisplayName("PATCH /api/v1/admin/orders/{orderId}/confirm - Confirmer une commande")
    class ConfirmOrderTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait confirmer une commande avec succès")
        void shouldConfirmOrderSuccessfully() throws Exception {
            // Arrange
            Long orderId = 1L;

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", orderId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode", is("200")))
                    .andExpect(jsonPath("$.statusMsg", containsString("Commande #1")))
                    .andExpect(jsonPath("$.statusMsg", containsString("approuvé")));

            verify(orderService).updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner 404 quand la commande n'existe pas")
        void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
            // Arrange
            Long orderId = 999L;
            doThrow(exceptionFactory.orderNotFound(orderId))
                    .when(orderService).updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);

            // Act & Assert - CORRECTION du message
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", orderId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Commande introuvable avec les données id : '999'"));

            verify(orderService).updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner 400 pour ID de commande invalide")
        void shouldReturnBadRequestForInvalidOrderId() throws Exception {
            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", "invalid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(orderService, never()).updateOrderStatus(anyLong(), anyString());
        }
    }

    // ANNULATION COMMANDE
    @Nested
    @DisplayName("PATCH /api/v1/admin/orders/{orderId}/cancel - Annuler une commande")
    class CancelOrderTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait annuler une commande avec succès")
        void shouldCancelOrderSuccessfully() throws Exception {
            // Arrange
            Long orderId = 1L;

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/cancel", orderId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode", is("200")))
                    .andExpect(jsonPath("$.statusMsg", containsString("Commande #1")))
                    .andExpect(jsonPath("$.statusMsg", containsString("annulé")));

            verify(orderService).updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CANCELLED);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner 404 quand la commande à annuler n'existe pas")
        void shouldReturnNotFoundWhenOrderToCancelDoesNotExist() throws Exception {
            // Arrange
            Long orderId = 999L;
            doThrow(exceptionFactory.orderNotFound(orderId))
                    .when(orderService).updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CANCELLED);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/cancel", orderId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(orderService).updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CANCELLED);
        }
    }

    //TESTS MESSAGES OUVERTS
    @Nested
    @DisplayName("GET /api/v1/admin/messages - Récupérer les messages ouverts")
    class GetAllOpenMessagesTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner la liste des messages ouverts")
        void shouldReturnAllOpenMessages() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/messages")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].contactId", is(1)))
                    .andExpect(jsonPath("$[1].contactId", is(2)));

            verify(contactService).getAllOpenMessages();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner une liste vide quand il n'y a pas de messages")
        void shouldReturnEmptyListWhenNoOpenMessages() throws Exception {
            // Arrange
            when(contactService.getAllOpenMessages()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/messages")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", empty()));

            verify(contactService).getAllOpenMessages();
        }
    }

    // TESTS FERMETURE MESSAGE
    @Nested
    @DisplayName("PATCH /api/v1/admin/messages/{contactId}/close - Fermer un message")
    class CloseMessageTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait fermer un message avec succès")
        void shouldCloseMessageSuccessfully() throws Exception {
            // Arrange
            Long contactId = 1L;

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/messages/{contactId}/close", contactId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode", is("200")))
                    .andExpect(jsonPath("$.statusMsg", containsString("Contact #1")))
                    .andExpect(jsonPath("$.statusMsg", containsString("fermé")));

            verify(contactService).updateMessageStatus(contactId, ApplicationConstants.CLOSED_MESSAGE);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner 404 quand le message n'existe pas")
        void shouldReturnNotFoundWhenMessageDoesNotExist() throws Exception {
            // Arrange
            Long contactId = 999L;
            doThrow(exceptionFactory.contactNotFound(contactId))
                    .when(contactService).updateMessageStatus(contactId, ApplicationConstants.CLOSED_MESSAGE);

            // Act & Assert - CORRECTION du message
            mockMvc.perform(patch("/api/v1/admin/messages/{contactId}/close", contactId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Contact introuvable avec les données id : '999'"));

            verify(contactService).updateMessageStatus(contactId, ApplicationConstants.CLOSED_MESSAGE);
        }
    }

    // TESTS DE ROBUSTESSE
    @Nested
    @DisplayName("Tests de robustesse")
    class RobustnessTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait gérer les exceptions business avec ExceptionFactory")
        void shouldHandleBusinessExceptionsWithFactory() throws Exception {
            // Arrange
            Long orderId = 1L;
            doThrow(exceptionFactory.businessError("Erreur métier spécifique"))
                    .when(orderService).updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", orderId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Erreur métier spécifique"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner 400 pour ID invalide")
        void shouldReturnBadRequestForInvalidId() throws Exception {
            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", "invalid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner 400 pour ID négatif")
        void shouldReturnBadRequestForNegativeId() throws Exception {
            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", "-1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Devrait refuser l'accès aux non-admins")
        void shouldDenyAccessForNonAdminUsers() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());

            verify(orderService, never()).getAllPendingOrders();
        }

        @Test
        @DisplayName("Devrait refuser l'accès aux utilisateurs non authentifiés")
        void shouldDenyAccessForUnauthenticatedUsers() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());

            verify(orderService, never()).getAllPendingOrders();
        }
    }
}