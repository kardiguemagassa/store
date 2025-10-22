package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.config.TestSecurityConfig;
import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.ContactResponseDto;
import com.store.store.dto.OrderResponseDto;
import com.store.store.exception.ContactNotFoundException;
import com.store.store.exception.OrderNotFoundException;
import com.store.store.service.IContactService;
import com.store.store.service.IOrderService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

    private List<OrderResponseDto> mockOrders;
    private List<ContactResponseDto> mockMessages;

    @BeforeEach
    void setUp() {
        mockOrders = Arrays.asList(
                TestDataBuilder.createOrderResponseDto(1L),
                TestDataBuilder.createOrderResponseDto(2L)
        );

        mockMessages = Arrays.asList(
                TestDataBuilder.createContactResponseDto(1L),
                TestDataBuilder.createContactResponseDto(2L)
        );

        when(orderService.getAllPendingOrders()).thenReturn(mockOrders);
        when(contactService.getAllOpenMessages()).thenReturn(mockMessages);
        doNothing().when(orderService).updateOrderStatus(anyLong(), anyString());
        doNothing().when(contactService).updateMessageStatus(anyLong(), anyString());
    }

    // ===== TESTS GESTION ERREURS MÉTIER =====

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait retourner 404 quand la commande n'existe pas")
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        // Arrange
        Long orderId = 999L;
        doThrow(new OrderNotFoundException(orderId))
                .when(orderService).updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("Not Found"))
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Commande introuvable avec les données id : '999'"));

        // Verify
        verify(orderService).updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait retourner 404 quand le message n'existe pas")
    void shouldReturnNotFoundWhenMessageDoesNotExist() throws Exception {
        // Arrange
        Long contactId = 999L;
        doThrow(new ContactNotFoundException(contactId))
                .when(contactService).updateMessageStatus(contactId, ApplicationConstants.CLOSED_MESSAGE);

        // Act & Assert - ✅ MESSAGE CORRIGÉ
        mockMvc.perform(patch("/api/v1/admin/messages/{contactId}/close", contactId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("Not Found"))
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Contact introuvable avec les données id : '999'")); // ✅ CORRIGÉ

        // Verify
        verify(contactService).updateMessageStatus(contactId, ApplicationConstants.CLOSED_MESSAGE);
    }

    // ===== TESTS COMMANDES EN ATTENTE =====

    @Nested
    @DisplayName("GET /api/v1/admin/orders - Récupérer toutes les commandes en attente")
    class GetAllPendingOrdersTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner la liste des commandes en attente avec succès")
        void shouldReturnAllPendingOrders() throws Exception {
            // Arrange
            when(orderService.getAllPendingOrders()).thenReturn(mockOrders);

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].orderId", is(1)))
                    .andExpect(jsonPath("$[0].status", is(ApplicationConstants.ORDER_STATUS_CREATED)))
                    .andExpect(jsonPath("$[0].totalPrice", is(175.00)))
                    .andExpect(jsonPath("$[0].items", hasSize(2)))
                    .andExpect(jsonPath("$[1].orderId", is(2)))
                    .andExpect(jsonPath("$[1].status", is(ApplicationConstants.ORDER_STATUS_CREATED)));

            // Verify
            verify(orderService, times(1)).getAllPendingOrders();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner une liste vide s'il n'y a pas de commandes en attente")
        void shouldReturnEmptyListWhenNoPendingOrders() throws Exception {
            // Arrange
            when(orderService.getAllPendingOrders()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)))
                    .andExpect(jsonPath("$", empty()));

            // Verify
            verify(orderService, times(1)).getAllPendingOrders();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Devrait refuser l'accès aux utilisateurs non-admin")
        void shouldDenyAccessForNonAdminUsers() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            // Verify
            verify(orderService, never()).getAllPendingOrders();
        }

        @Test
        @DisplayName("Devrait refuser l'accès aux utilisateurs non authentifiés")
        void shouldDenyAccessForUnauthenticatedUsers() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());

            // Verify
            verify(orderService, never()).getAllPendingOrders();
        }
    }

    // ===== TESTS CONFIRMATION COMMANDE =====

    @Nested
    @DisplayName("PATCH /api/v1/admin/orders/{orderId}/confirm - Confirmer une commande")
    class ConfirmOrderTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait confirmer une commande avec succès")
        void shouldConfirmOrderSuccessfully() throws Exception {
            // Arrange
            Long orderId = 1L;
            doNothing().when(orderService).updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", orderId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.statusCode", is("200")))
                    .andExpect(jsonPath("$.statusMsg", containsString("Commande #1")))
                    .andExpect(jsonPath("$.statusMsg", containsString("approuvé")));

            // Verify
            verify(orderService, times(1)).updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait confirmer plusieurs commandes successivement")
        void shouldConfirmMultipleOrdersSuccessively() throws Exception {
            // Arrange
            doNothing().when(orderService).updateOrderStatus(anyLong(), eq(ApplicationConstants.ORDER_STATUS_CONFIRMED));

            // Act & Assert - Première commande
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusMsg", containsString("Commande #1")));

            // Act & Assert - Deuxième commande
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", 2L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusMsg", containsString("Commande #2")));

            // Verify
            verify(orderService, times(2)).updateOrderStatus(anyLong(), eq(ApplicationConstants.ORDER_STATUS_CONFIRMED));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Devrait refuser l'accès aux utilisateurs non-admin")
        void shouldDenyAccessForNonAdminUsers() throws Exception {
            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", 1L)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            // Verify
            verify(orderService, never()).updateOrderStatus(anyLong(), anyString());
        }

        @Test
        @DisplayName("Devrait refuser l'accès aux utilisateurs non authentifiés")
        void shouldDenyAccessForUnauthenticatedUsers() throws Exception {
            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", 1L)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());

            // Verify
            verify(orderService, never()).updateOrderStatus(anyLong(), anyString());
        }
    }

    // ===== TESTS ANNULATION COMMANDE =====

    @Nested
    @DisplayName("PATCH /api/v1/admin/orders/{orderId}/cancel - Annuler une commande")
    class CancelOrderTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait annuler une commande avec succès")
        void shouldCancelOrderSuccessfully() throws Exception {
            // Arrange
            Long orderId = 1L;
            doNothing().when(orderService).updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CANCELLED);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/cancel", orderId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.statusCode", is("200")))
                    .andExpect(jsonPath("$.statusMsg", containsString("Commande #1")))
                    .andExpect(jsonPath("$.statusMsg", containsString("annulé")));

            // Verify
            verify(orderService, times(1)).updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CANCELLED);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Devrait refuser l'accès aux utilisateurs non-admin")
        void shouldDenyAccessForNonAdminUsers() throws Exception {
            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/cancel", 1L)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            // Verify
            verify(orderService, never()).updateOrderStatus(anyLong(), anyString());
        }
    }

    // ===== TESTS MESSAGES OUVERTS =====

    @Nested
    @DisplayName("GET /api/v1/admin/messages - Récupérer tous les messages ouverts")
    class GetAllOpenMessagesTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner la liste des messages ouverts avec succès")
        void shouldReturnAllOpenMessages() throws Exception {
            // Arrange
            when(contactService.getAllOpenMessages()).thenReturn(mockMessages);

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/messages")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].contactId", is(1)))
                    .andExpect(jsonPath("$[0].name", is("John Smith")))
                    .andExpect(jsonPath("$[0].email", is("john.smith@example.com")))
                    .andExpect(jsonPath("$[0].mobileNumber", is("0612345678")))
                    .andExpect(jsonPath("$[0].status", is(ApplicationConstants.OPEN_MESSAGE)))
                    .andExpect(jsonPath("$[1].contactId", is(2)));

            // Verify
            verify(contactService, times(1)).getAllOpenMessages();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner une liste vide s'il n'y a pas de messages ouverts")
        void shouldReturnEmptyListWhenNoOpenMessages() throws Exception {
            // Arrange
            when(contactService.getAllOpenMessages()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/messages")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)))
                    .andExpect(jsonPath("$", empty()));

            // Verify
            verify(contactService, times(1)).getAllOpenMessages();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Devrait refuser l'accès aux utilisateurs non-admin")
        void shouldDenyAccessForNonAdminUsers() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/messages")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            // Verify
            verify(contactService, never()).getAllOpenMessages();
        }
    }

    // ===== TESTS FERMETURE MESSAGE =====

    @Nested
    @DisplayName("PATCH /api/v1/admin/messages/{contactId}/close - Fermer un message")
    class CloseMessageTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait fermer un message avec succès")
        void shouldCloseMessageSuccessfully() throws Exception {
            // Arrange
            Long contactId = 1L;
            doNothing().when(contactService).updateMessageStatus(contactId, ApplicationConstants.CLOSED_MESSAGE);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/messages/{contactId}/close", contactId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.statusCode", is("200")))
                    .andExpect(jsonPath("$.statusMsg", containsString("Contact #1")))
                    .andExpect(jsonPath("$.statusMsg", containsString("fermé")));

            // Verify
            verify(contactService, times(1)).updateMessageStatus(contactId, ApplicationConstants.CLOSED_MESSAGE);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait fermer plusieurs messages successivement")
        void shouldCloseMultipleMessagesSuccessively() throws Exception {
            // Arrange
            doNothing().when(contactService).updateMessageStatus(anyLong(), eq(ApplicationConstants.CLOSED_MESSAGE));

            // Act & Assert - Premier message
            mockMvc.perform(patch("/api/v1/admin/messages/{contactId}/close", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusMsg", containsString("Contact #1")));

            // Act & Assert - Deuxième message
            mockMvc.perform(patch("/api/v1/admin/messages/{contactId}/close", 2L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusMsg", containsString("Contact #2")));

            // Verify
            verify(contactService, times(2)).updateMessageStatus(anyLong(), eq(ApplicationConstants.CLOSED_MESSAGE));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Devrait refuser l'accès aux utilisateurs non-admin")
        void shouldDenyAccessForNonAdminUsers() throws Exception {
            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/messages/{contactId}/close", 1L)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            // Verify
            verify(contactService, never()).updateMessageStatus(anyLong(), anyString());
        }
    }

    // ===== TESTS DE ROBUSTESSE CORRIGÉS =====

    @Nested
    @DisplayName("Tests de robustesse")
    class RobustnessTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait gérer les IDs numériques très grands")
        void shouldHandleVeryLargeNumericIds() throws Exception {
            // Arrange
            Long largeOrderId = 9_999_999_999L;
            doNothing().when(orderService).updateOrderStatus(largeOrderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", largeOrderId))
                    .andExpect(status().isOk());

            // Verify
            verify(orderService).updateOrderStatus(largeOrderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner 400 pour ID invalide")
        void shouldReturnBadRequestForInvalidId() throws Exception {
            // Act & Assert - ✅ CORRIGÉ : attend 400 au lieu de 500
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", "invalid-id"))
                    .andExpect(status().isBadRequest()) // ✅ 400 au lieu de 500
                    .andExpect(jsonPath("$.message").value(containsString("paramètre"))); // Message de type mismatch
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Devrait retourner 400 pour ID négatif")
        void shouldReturnBadRequestForNegativeId() throws Exception {
            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/confirm", "-1"))
                    .andExpect(status().isBadRequest());
        }
    }
}