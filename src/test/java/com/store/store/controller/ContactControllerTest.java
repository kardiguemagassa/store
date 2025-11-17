package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.config.TestSecurityConfig;
import com.store.store.dto.contact.ContactInfoDto;
import com.store.store.dto.contact.ContactRequestDto;
import com.store.store.service.IContactService;
import com.store.store.util.TestDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests Unitaires pour ContactController
 * Utilise des mocks pour isoler le controller du service
 */
@WebMvcTest(ContactController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Tests Unitaires - ContactController")
@Slf4j
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IContactService contactService;

    @MockitoBean
    private ContactInfoDto contactInfoDto;
    @MockitoBean
    private MessageSource messageSource = mock(MessageSource.class);

    @BeforeEach
    void setUp() {
        // Configurer le MessageSource pour retourner le message attendu
        when(messageSource.getMessage(
                eq("success.contact.created"),
                any(Object[].class),
                any()))               // ← Le Locale sera injecté automatiquement
                .thenReturn("Votre message a été envoyé avec succès");
    }

    //POST /api/v1/contacts
    @Test
    @DisplayName("POST /api/v1/contacts - Devrait créer un contact avec succès")
    @WithMockUser
    void saveContact_WithValidData_ShouldReturnCreated() throws Exception {
        // Given
        ContactRequestDto requestDto = TestDataBuilder.createContactRequestDto();
        String requestJson = objectMapper.writeValueAsString(requestDto);

        when(contactService.saveContact(any(ContactRequestDto.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Votre message a été envoyé avec succès"))
                .andExpect(jsonPath("$.status").value(201));

        verify(contactService, times(1)).saveContact(any(ContactRequestDto.class));
        log.info("Test POST contact réussi");
    }

    @Test
    @DisplayName("POST /api/v1/contacts - Devrait échouer avec nom trop court")
    @WithMockUser
    void saveContact_WithShortName_ShouldReturnBadRequest() throws Exception {
        // Given
        ContactRequestDto requestDto = ContactRequestDto.builder()
                .name("John")  // Moins de 5 caractères
                .email("john.doe@example.com")
                .mobileNumber("0612345678")
                .message("This is a test message")
                .build();

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
        log.info("Validation nom trop court vérifiée");
    }

    @Test
    @DisplayName("POST /api/v1/contacts - Devrait échouer avec nom vide")
    @WithMockUser
    void saveContact_WithEmptyName_ShouldReturnBadRequest() throws Exception {
        // Given
        ContactRequestDto requestDto = ContactRequestDto.builder()
                .name("")
                .email("john.doe@example.com")
                .mobileNumber("0612345678")
                .message("This is a test message")
                .build();

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
        log.info("Validation nom vide vérifiée");
    }

    @Test
    @DisplayName("POST /api/v1/contacts - Devrait échouer avec email invalide")
    @WithMockUser
    void saveContact_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        // Given
        ContactRequestDto requestDto = ContactRequestDto.builder()
                .name("John Doe")
                .email("invalid-email")
                .mobileNumber("0612345678")
                .message("This is a test message")
                .build();

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
        log.info("Validation email invalide vérifiée");
    }

    @Test
    @DisplayName("POST /api/v1/contacts - Devrait échouer avec numéro de téléphone invalide")
    @WithMockUser
    void saveContact_WithInvalidPhone_ShouldReturnBadRequest() throws Exception {
        // Given
        ContactRequestDto requestDto = ContactRequestDto.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .mobileNumber("123")  // Moins de 10 chiffres
                .message("This is a test message")
                .build();

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
        log.info("Validation téléphone invalide vérifiée");
    }

    @Test
    @DisplayName("POST /api/v1/contacts - Devrait échouer avec message trop court")
    @WithMockUser
    void saveContact_WithShortMessage_ShouldReturnBadRequest() throws Exception {
        // Given
        ContactRequestDto requestDto = ContactRequestDto.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .mobileNumber("0612345678")
                .message("Hi")  // Moins de 5 caractères
                .build();

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
        log.info("Validation message trop court vérifiée");
    }

    @Test
    @DisplayName("POST /api/v1/contacts - Devrait échouer avec message vide")
    @WithMockUser
    void saveContact_WithEmptyMessage_ShouldReturnBadRequest() throws Exception {
        // Given
        ContactRequestDto requestDto = ContactRequestDto.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .mobileNumber("0612345678")
                .message("")
                .build();

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
        log.info("Validation message vide vérifiée");
    }

    @Test
    @DisplayName("POST /api/v1/contacts - Devrait échouer avec tous les champs vides")
    @WithMockUser
    void saveContact_WithAllEmptyFields_ShouldReturnBadRequest() throws Exception {
        // Given
        ContactRequestDto requestDto = ContactRequestDto.builder()
                .name("")
                .email("")
                .mobileNumber("")
                .message("")
                .build();

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
        log.info("Validation tous champs vides vérifiée");
    }

    @Test
    @DisplayName("POST /api/v1/contacts - Devrait gérer les erreurs du service")
    @WithMockUser
    void saveContact_WhenServiceFails_ShouldReturnError() throws Exception {
        // Given
        ContactRequestDto requestDto = TestDataBuilder.createContactRequestDto();
        String requestJson = objectMapper.writeValueAsString(requestDto);

        when(contactService.saveContact(any(ContactRequestDto.class)))
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(contactService, times(1)).saveContact(any(ContactRequestDto.class));
        log.info("Gestion erreur service vérifiée");
    }

    @Test
    @DisplayName("POST /api/v1/contacts - Devrait échouer sans authentification")
    void saveContact_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        // Given
        ContactRequestDto requestDto = TestDataBuilder.createContactRequestDto();
        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(contactService);
        log.info("Sécurité POST contact vérifiée");
    }

    // GET /api/v1/contacts
    @Test
    @DisplayName("GET /api/v1/contacts - Devrait retourner les informations de contact")
    @WithMockUser
    void getContactInfo_ShouldReturnContactInfo() throws Exception {
        // Given
        when(contactInfoDto.phone()).thenReturn("0123456789");
        when(contactInfoDto.email()).thenReturn("contact@store.com");
        when(contactInfoDto.address()).thenReturn("123 Main Street, Paris");

        // When & Then
        mockMvc.perform(get("/api/v1/contacts"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("0123456789"))
                .andExpect(jsonPath("$.email").value("contact@store.com"))
                .andExpect(jsonPath("$.address").value("123 Main Street, Paris"));

        log.info("Test GET contact info réussi");
    }

    @Test
    @DisplayName("GET /api/v1/contacts - Devrait être accessible sans authentification")
    void getContactInfo_WithoutAuth_ShouldSucceed() throws Exception {
        // Given
        when(contactInfoDto.phone()).thenReturn("0123456789");
        when(contactInfoDto.email()).thenReturn("contact@store.com");
        when(contactInfoDto.address()).thenReturn("123 Main Street, Paris");

        // When & Then
        mockMvc.perform(get("/api/v1/contacts"))
                .andDo(print())
                .andExpect(status().isOk());

        log.info("Endpoint public GET contact info vérifié");
    }

    @Test
    @DisplayName("GET /api/v1/contacts - Devrait retourner le bon Content-Type")
    @WithMockUser
    void getContactInfo_ShouldReturnJsonContentType() throws Exception {
        // Given
        when(contactInfoDto.phone()).thenReturn("0123456789");
        when(contactInfoDto.email()).thenReturn("contact@store.com");
        when(contactInfoDto.address()).thenReturn("123 Main Street, Paris");

        // When & Then
        mockMvc.perform(get("/api/v1/contacts"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        log.info("Content-Type JSON vérifié");
    }

    @Test
    @DisplayName("GET /api/v1/contacts - Devrait retourner tous les champs requis")
    @WithMockUser
    void getContactInfo_ShouldReturnAllRequiredFields() throws Exception {
        // Given
        when(contactInfoDto.phone()).thenReturn("0123456789");
        when(contactInfoDto.email()).thenReturn("contact@store.com");
        when(contactInfoDto.address()).thenReturn("123 Main Street, Paris");

        // When & Then
        mockMvc.perform(get("/api/v1/contacts"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.address").exists())
                .andExpect(jsonPath("$.phone").isString())
                .andExpect(jsonPath("$.email").isString())
                .andExpect(jsonPath("$.address").isString());

        log.info("Tous les champs requis présents");
    }

    //CONTENU
    @Test
    @DisplayName("POST /api/v1/contacts - Devrait rejeter JSON malformé")
    @WithMockUser
    void saveContact_WithMalformedJson_ShouldReturnBadRequest() throws Exception {
        // Given
        String malformedJson = "{invalid json}";

        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
        log.info("JSON malformé rejeté");
    }

    @Test
    @DisplayName("POST /api/v1/contacts - Devrait rejeter Content-Type invalide")
    @WithMockUser
    void saveContact_WithInvalidContentType_ShouldReturnBadRequest() throws Exception {
        // Given
        String content = "invalid content";

        // When & Then - devrait retourner 415 grâce au nouveau handler
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(content))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.statusCode").value(415));
                //.andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(contactService);
        log.info("Content-Type invalide rejeté avec Unsupported Media Type (415)");
    }

    @Test
    @DisplayName("POST /api/v1/contacts - Devrait appeler le service avec les bonnes données")
    @WithMockUser
    void saveContact_ShouldCallServiceWithCorrectData() throws Exception {
        // Given
        ContactRequestDto requestDto = TestDataBuilder.createContactRequestDto();
        String requestJson = objectMapper.writeValueAsString(requestDto);

        when(contactService.saveContact(any(ContactRequestDto.class))).thenReturn(true);

        // When
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        // Then
        verify(contactService, times(1)).saveContact(any(ContactRequestDto.class));
        verify(contactService, only()).saveContact(any(ContactRequestDto.class));
        log.info("Appel service avec données correctes vérifié");
    }

    @Test
    @DisplayName("POST /api/v1/contacts - Devrait valider la taille maximale du nom")
    @WithMockUser
    void saveContact_WithTooLongName_ShouldReturnBadRequest() throws Exception {
        // Given - Nom de 31 caractères (max = 30)
        ContactRequestDto requestDto = ContactRequestDto.builder()
                .name("A".repeat(31))
                .email("john.doe@example.com")
                .mobileNumber("0612345678")
                .message("This is a test message")
                .build();

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
        log.info("Validation taille maximale nom vérifiée");
    }

    @Test
    @DisplayName("POST /api/v1/contacts - Devrait valider la taille maximale du message")
    @WithMockUser
    void saveContact_WithTooLongMessage_ShouldReturnBadRequest() throws Exception {
        // Given - Message de 501 caractères (max = 500)
        ContactRequestDto requestDto = ContactRequestDto.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .mobileNumber("0612345678")
                .message("A".repeat(501))
                .build();

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
        log.info("Validation taille maximale message vérifiée");
    }
}