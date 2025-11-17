package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.contact.ContactRequestDto;
import com.store.store.entity.Contact;
import com.store.store.repository.ContactRepository;
import com.store.store.util.TestDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'Intégration pour ContactController
 * Teste le flux complet avec la base de données
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Slf4j
@DisplayName("Tests d'Intégration - ContactController")
class ContactControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContactRepository contactRepository;

    @BeforeEach
    void setUp() {
        contactRepository.deleteAll();
        log.info("Configuration test terminée - Base de données nettoyée");
    }

    //TESTS POST /api/v1/contacts
    @Test
    @DisplayName("POST /api/v1/contacts - Devrait persister le contact en base")
    @WithMockUser
    void saveContact_ShouldPersistToDatabase() throws Exception {
        // Given
        ContactRequestDto requestDto = TestDataBuilder.createContactRequestDto();
        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .characterEncoding( "UTF-8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());

        // Then - Vérifier en base
        var contacts = contactRepository.findAll();
        assert contacts.size() == 1;

        Contact savedContact = contacts.getFirst();
        assert savedContact.getName().equals("John Smith");
        assert savedContact.getEmail().equals("john.smith@example.com");
        assert savedContact.getMobileNumber().equals("0612345678");
        assert savedContact.getMessage().equals("This is a test message for contact support.");
        assert savedContact.getStatus().equals(ApplicationConstants.OPEN_MESSAGE);

        log.info("Contact persisté en base avec succès");
    }

    @Test
    @DisplayName("POST /api/v1/contacts - Plusieurs contacts devraient être persistés")
    @WithMockUser
    void saveContact_MultipleContacts_ShouldPersistAll() throws Exception {
        // Given
        ContactRequestDto request1 = ContactRequestDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .mobileNumber("0612345678")
                .message("Message 1 de test pour le support")
                .build();

        ContactRequestDto request2 = ContactRequestDto.builder()
                .name("Jane Smith")
                .email("jane@example.com")
                .mobileNumber("0698765432")
                .message("Message 2 de test pour le support")
                .build();

        // When
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // Then
        var contacts = contactRepository.findAll();
        assert contacts.size() == 2;

        log.info("Multiples contacts persistés avec succès");
    }

    // TESTS GET /api/v1/contacts
    @Test
    @DisplayName("GET /api/v1/contacts - Devrait retourner les informations de contact configurées")
    void getContactInfo_ShouldReturnConfiguredInfo() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/contacts"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.address").exists());

        log.info("Informations de contact retournées avec succès");
    }

    @Test
    @DisplayName("GET /api/v1/contacts - Devrait être accessible publiquement")
    void getContactInfo_WithoutAuth_ShouldBeAccessible() throws Exception {
        // When & Then - Sans authentification
        mockMvc.perform(get("/api/v1/contacts"))
                .andDo(print())
                .andExpect(status().isOk());

        log.info("Endpoint public accessible sans authentification");
    }

    //VALIDATION
    @Test
    @DisplayName("POST /api/v1/contacts - Devrait valider le format de l'email")
    @WithMockUser
    void saveContact_WithInvalidEmailFormat_ShouldReturnBadRequest() throws Exception {
        // Given
        ContactRequestDto requestDto = ContactRequestDto.builder()
                .name("John Doe")
                .email("invalid-email")
                .mobileNumber("0612345678")
                .message("This is a valid message for support")
                .build();

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Vérifier qu'aucun contact n'a été sauvegardé
        assert contactRepository.findAll().isEmpty();

        log.info("Validation email invalide vérifiée");
    }

    //PERSISTANCE
    @Test
    @DisplayName("Devrait vérifier la persistance complète des données")
    @WithMockUser
    void shouldVerifyCompleteDataPersistence() throws Exception {
        // Given
        ContactRequestDto requestDto = ContactRequestDto.builder()
                .name("Persistent Contact")
                .email("persist@example.com")
                .mobileNumber("0612345678")
                .message("This message should be persisted in database correctly")
                .build();

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        // Then - Vérification détaillée en base
        Contact savedContact = contactRepository.findAll().get(0);

        assert savedContact.getContactId() != null;
        assert savedContact.getName().equals("Persistent Contact");
        assert savedContact.getEmail().equals("persist@example.com");
        assert savedContact.getMobileNumber().equals("0612345678");
        assert savedContact.getMessage().equals("This message should be persisted in database correctly");
        assert savedContact.getStatus().equals(ApplicationConstants.OPEN_MESSAGE);
        assert savedContact.getCreatedAt() != null;
        assert savedContact.getCreatedBy() != null;

        log.info("Persistance complète des données vérifiée");
    }

    //TYPES DE DONNÉES
    @Test
    @DisplayName("GET /api/v1/contacts - Devrait retourner les bons types de données")
    void getContactInfo_ShouldReturnCorrectDataTypes() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/contacts"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").isString())
                .andExpect(jsonPath("$.email").isString())
                .andExpect(jsonPath("$.address").isString());

        log.info("Types de données vérifiés avec succès");
    }
}