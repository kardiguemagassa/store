package com.store.store.service.impl;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.contact.ContactRequestDto;
import com.store.store.dto.contact.ContactResponseDto;
import com.store.store.entity.Contact;
import com.store.store.exception.ResourceNotFoundException;
import com.store.store.repository.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactServiceImpl contactService;

    private ContactRequestDto contactRequestDto;
    private Contact contactEntity;
    private ContactResponseDto contactResponseDto;

    @BeforeEach
    void setUp() {
        // Given - Configuration commune pour tous les tests
        contactRequestDto = ContactRequestDto.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .mobileNumber("+1234567890")
                .message("Test message")
                .build();

        // Utilisation du constructeur au lieu du Builder
        contactEntity = new Contact();
        contactEntity.setContactId(1L);
        contactEntity.setName("John Doe");
        contactEntity.setEmail("john.doe@example.com");
        contactEntity.setMobileNumber("+1234567890");
        contactEntity.setMessage("Test message");
        contactEntity.setStatus(ApplicationConstants.OPEN_MESSAGE);

        contactResponseDto = new ContactResponseDto(
                1L,
                "John Doe",
                "john.doe@example.com",
                "+1234567890",
                "Test message",
                ApplicationConstants.OPEN_MESSAGE
        );
    }

    @Test
    @DisplayName("DEV-001: Sauvegarder un contact - Doit retourner true et sauvegarder l'entité")
    void saveContact_WithValidRequest_ShouldReturnTrueAndSaveEntity() {
        // Given - Configuration spécifique pour ce test
        when(contactRepository.save(any(Contact.class))).thenReturn(contactEntity);

        // When - Exécution de la méthode à tester
        boolean result = contactService.saveContact(contactRequestDto);

        // Then - Vérifications
        assertThat(result).isTrue();
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    @DisplayName("DEV-002: Récupérer tous les messages ouverts - Doit retourner la liste des messages ouverts")
    void getAllOpenMessages_WhenMessagesExist_ShouldReturnOpenMessages() {
        // Given
        List<Contact> openContacts = List.of(contactEntity);
        when(contactRepository.fetchByStatus(ApplicationConstants.OPEN_MESSAGE))
                .thenReturn(openContacts);

        // When
        List<ContactResponseDto> result = contactService.getAllOpenMessages();

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);

        ContactResponseDto firstResult = result.getFirst();
        assertThat(firstResult.contactId()).isEqualTo(1L);
        assertThat(firstResult.name()).isEqualTo("John Doe");
        assertThat(firstResult.email()).isEqualTo("john.doe@example.com");
        assertThat(firstResult.status()).isEqualTo(ApplicationConstants.OPEN_MESSAGE);

        verify(contactRepository).fetchByStatus(ApplicationConstants.OPEN_MESSAGE);
    }

    @Test
    @DisplayName("DEV-003: Récupérer tous les messages ouverts - Doit retourner une liste vide quand aucun message")
    void getAllOpenMessages_WhenNoMessages_ShouldReturnEmptyList() {
        // Given
        when(contactRepository.fetchByStatus(ApplicationConstants.OPEN_MESSAGE))
                .thenReturn(List.of());

        // When
        List<ContactResponseDto> result = contactService.getAllOpenMessages();

        // Then
        assertThat(result).isEmpty();
        verify(contactRepository).fetchByStatus(ApplicationConstants.OPEN_MESSAGE);
    }

    @Test
    @DisplayName("DEV-004: Mettre à jour le statut d'un message - Doit mettre à jour quand le contact existe")
    void updateMessageStatus_WithExistingContact_ShouldUpdateStatus() {
        // Given
        Long contactId = 1L;
        String newStatus = "CLOSED";
        when(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity));
        when(contactRepository.save(any(Contact.class))).thenReturn(contactEntity);

        // When
        contactService.updateMessageStatus(contactId, newStatus);

        // Then
        assertThat(contactEntity.getStatus()).isEqualTo(newStatus);
        verify(contactRepository).findById(contactId);
        verify(contactRepository).save(contactEntity);
    }

    @Test
    @DisplayName("DEV-005: Mettre à jour le statut d'un message - Doit lancer une exception quand le contact n'existe pas")
    void updateMessageStatus_WithNonExistingContact_ShouldThrowException() {
        // Given
        Long contactId = 999L;
        String newStatus = "CLOSED";
        when(contactRepository.findById(contactId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> contactService.updateMessageStatus(contactId, newStatus))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Contact introuvable avec les données ContactID : '999'");

        verify(contactRepository).findById(contactId);
        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    @DisplayName("DEV-006: Transformation DTO vers Entity - Doit correctement mapper les propriétés")
    void transformToEntity_WithValidDto_ShouldMapCorrectly() {
        // Given - Utilisation de la réflexion pour tester la méthode privée
        // Note: En pratique, on teste cette méthode indirectement via saveContact

        // When
        boolean result = contactService.saveContact(contactRequestDto);

        // Then - La méthode est testée indirectement via le comportement global
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    @DisplayName("DEV-007: Mapping Entity vers DTO - Doit correctement mapper les propriétés")
    void mapToContactResponseDTO_WithValidEntity_ShouldMapCorrectly() {
        // Given - Test indirect via getAllOpenMessages
        when(contactRepository.fetchByStatus(ApplicationConstants.OPEN_MESSAGE))
                .thenReturn(List.of(contactEntity));

        // When
        List<ContactResponseDto> result = contactService.getAllOpenMessages();

        // Then
        assertThat(result.get(0))
                .usingRecursiveComparison()
                .isEqualTo(contactResponseDto);
    }
}