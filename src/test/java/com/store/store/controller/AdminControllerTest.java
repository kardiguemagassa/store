/*package com.store.store.controller;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.ContactResponseDto;
import com.store.store.dto.OrderResponseDto;
import com.store.store.dto.ResponseDto;
import com.store.store.service.IContactService;
import com.store.store.service.IOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - AdminController")
class AdminControllerTest {

    @Mock
    private IOrderService orderService;

    @Mock
    private IContactService contactService;

    @InjectMocks
    private AdminController adminController;

    private OrderResponseDto orderDto1;
    private OrderResponseDto orderDto2;
    private ContactResponseDto contactDto1;

    @BeforeEach
    void setUp() {
        // Initialisation des données de test
        orderDto1 = OrderResponseDto.builder()
                .orderId(1L)
                .status(ApplicationConstants.ORDER_STATUS_PENDING)
                .totalPrice(BigDecimal.valueOf(100.0))
                .createdAt(LocalDateTime.now().toString())
                .build();

        orderDto2 = OrderResponseDto.builder()
                .orderId(2L)
                .status(ApplicationConstants.ORDER_STATUS_PENDING)
                .totalPrice(BigDecimal.valueOf(200.0))
                .createdAt(LocalDateTime.now().toString())
                .build();

        contactDto1 = ContactResponseDto.builder()
                .contactId(1L)
                .status(ApplicationConstants.OPEN_MESSAGE)
                .message("Question about product")
                .email("user@example.com")
                .build();
    }

    @Test
    @DisplayName("getAllPendingOrders - Devrait retourner toutes les commandes en attente")
    void getAllPendingOrders_ShouldReturnAllPendingOrders() {
        // Arrange
        List<OrderResponseDto> expectedOrders = Arrays.asList(orderDto1, orderDto2);
        when(orderService.getAllPendingOrders()).thenReturn(expectedOrders);

        // Act
        ResponseEntity<List<OrderResponseDto>> response = adminController.getAllPendingOrders();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).containsExactlyInAnyOrder(orderDto1, orderDto2);

        verify(orderService, times(1)).getAllPendingOrders();
    }

    @Test
    @DisplayName("getAllPendingOrders - Devrait retourner une liste vide si aucune commande")
    void getAllPendingOrders_ShouldReturnEmptyList_WhenNoOrders() {
        // Arrange
        when(orderService.getAllPendingOrders()).thenReturn(List.of());

        // Act
        ResponseEntity<List<OrderResponseDto>> response = adminController.getAllPendingOrders();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();

        verify(orderService, times(1)).getAllPendingOrders();
    }

    @Test
    @DisplayName("confirmOrder - Devrait confirmer une commande avec succès")
    void confirmOrder_ShouldConfirmOrder_Successfully() {
        // Arrange
        Long orderId = 1L;
        doNothing().when(orderService).updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);

        // Act
        ResponseEntity<ResponseDto> response = adminController.confirmOrder(orderId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        //assertThat(response.getBody().getStatusCode()).isEqualTo("200");
        assertThat(response.getBody().statusCode()).isEqualTo("200");
        assertThat(response.getBody().statusMsg())
                .contains("Commande #" + orderId)
                .contains("approuvé");

        verify(orderService, times(1))
                .updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);
    }

    @Test
    @DisplayName("confirmOrder - Devrait appeler le service avec les bons paramètres")
    void confirmOrder_ShouldCallServiceWithCorrectParameters() {
        // Arrange
        Long orderId = 5L;

        // Act
        adminController.confirmOrder(orderId);

        // Assert
        verify(orderService).updateOrderStatus(
                eq(orderId),
                eq(ApplicationConstants.ORDER_STATUS_CONFIRMED)
        );
    }

    @Test
    @DisplayName("cancelOrder - Devrait annuler une commande avec succès")
    void cancelOrder_ShouldCancelOrder_Successfully() {
        // Arrange
        Long orderId = 2L;
        doNothing().when(orderService)
                .updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CANCELLED);

        // Act
        ResponseEntity<ResponseDto> response = adminController.cancelOrder(orderId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusCode()).isEqualTo("200");
        assertThat(response.getBody().statusMsg())
                .contains("Commande #" + orderId)
                .contains("annulé");

        verify(orderService, times(1))
                .updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CANCELLED);
    }

    @Test
    @DisplayName("cancelOrder - Devrait appeler le service avec les bons paramètres")
    void cancelOrder_ShouldCallServiceWithCorrectParameters() {
        // Arrange
        Long orderId = 10L;

        // Act
        adminController.cancelOrder(orderId);

        // Assert
        verify(orderService).updateOrderStatus(
                eq(orderId),
                eq(ApplicationConstants.ORDER_STATUS_CANCELLED)
        );
    }

    @Test
    @DisplayName("getAllOpenMessages - Devrait retourner tous les messages ouverts")
    void getAllOpenMessages_ShouldReturnAllOpenMessages() {
        // Arrange
        List<ContactResponseDto> expectedMessages = Arrays.asList(contactDto1);
        when(contactService.getAllOpenMessages()).thenReturn(expectedMessages);

        // Act
        ResponseEntity<List<ContactResponseDto>> response = adminController.getAllOpenMessages();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()).contains(contactDto1);

        verify(contactService, times(1)).getAllOpenMessages();
    }

    @Test
    @DisplayName("getAllOpenMessages - Devrait retourner une liste vide si aucun message")
    void getAllOpenMessages_ShouldReturnEmptyList_WhenNoMessages() {
        // Arrange
        when(contactService.getAllOpenMessages()).thenReturn(List.of());

        // Act
        ResponseEntity<List<ContactResponseDto>> response = adminController.getAllOpenMessages();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();

        verify(contactService, times(1)).getAllOpenMessages();
    }

    @Test
    @DisplayName("closeMessage - Devrait fermer un message avec succès")
    void closeMessage_ShouldCloseMessage_Successfully() {
        // Arrange
        Long contactId = 1L;
        doNothing().when(contactService)
                .updateMessageStatus(contactId, ApplicationConstants.CLOSED_MESSAGE);

        // Act
        ResponseEntity<ResponseDto> response = adminController.closeMessage(contactId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusCode()).isEqualTo("200");
        assertThat(response.getBody().statusMsg())
                .contains("Contact #" + contactId)
                .contains("fermé");

        verify(contactService, times(1))
                .updateMessageStatus(contactId, ApplicationConstants.CLOSED_MESSAGE);
    }

    @Test
    @DisplayName("closeMessage - Devrait appeler le service avec les bons paramètres")
    void closeMessage_ShouldCallServiceWithCorrectParameters() {
        // Arrange
        Long contactId = 15L;

        // Act
        adminController.closeMessage(contactId);

        // Assert
        verify(contactService).updateMessageStatus(
                eq(contactId),
                eq(ApplicationConstants.CLOSED_MESSAGE)
        );
    }
}*/
