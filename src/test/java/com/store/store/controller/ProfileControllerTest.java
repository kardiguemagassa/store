package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.config.TestSecurityConfig;
import com.store.store.dto.AddressDto;
import com.store.store.dto.ProfileRequestDto;
import com.store.store.dto.ProfileResponseDto;
import com.store.store.service.IProfileService;
import com.store.store.util.TestDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests Unitaires pour ProfileController
 * Utilise des mocks pour isoler le controller du service
 */
@WebMvcTest(ProfileController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Tests Unitaires - ProfileController")
@Slf4j
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IProfileService profileService;

    // ==================== TESTS GET /api/v1/profile ====================

    @Test
    @DisplayName("GET /api/v1/profile - Devrait retourner le profil")
    @WithMockUser
    void getProfile_ShouldReturnProfile() throws Exception {
        // Given
        ProfileResponseDto responseDto = TestDataBuilder.createProfileResponseDto(
                1L, "John Doe", "john@example.com", "0612345678", null);

        when(profileService.getProfile()).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/profile"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.mobileNumber").value("0612345678"));

        verify(profileService, times(1)).getProfile();
        log.info("✅ Test GET profile réussi");
    }

    @Test
    @DisplayName("GET /api/v1/profile - Devrait retourner le profil avec adresse")
    @WithMockUser
    void getProfile_WithAddress_ShouldReturnProfileWithAddress() throws Exception {
        // Given
        AddressDto addressDto = new AddressDto();
        addressDto.setCity("Paris");
        addressDto.setCountry("FR");
        addressDto.setPostalCode("75001");
        addressDto.setState("IDF");
        addressDto.setStreet("123 Main Street");

        ProfileResponseDto responseDto = TestDataBuilder.createProfileResponseDto(
                1L, "John Doe", "john@example.com", "0612345678", addressDto);

        when(profileService.getProfile()).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/profile"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address.street").value("123 Main Street"))
                .andExpect(jsonPath("$.address.city").value("Paris"));

        verify(profileService, times(1)).getProfile();
        log.info("✅ Test GET profile avec adresse réussi");
    }

    @Test
    @DisplayName("GET /api/v1/profile - Devrait échouer sans authentification")
    void getProfile_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/profile"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(profileService);
        log.info("✅ Test sécurité GET profile réussi");
    }

    @Test
    @DisplayName("GET /api/v1/profile - Devrait gérer les erreurs du service")
    @WithMockUser
    void getProfile_WhenServiceFails_ShouldReturnError() throws Exception {
        // Given
        when(profileService.getProfile())
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(get("/api/v1/profile"))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(profileService, times(1)).getProfile();
        log.info("✅ Test gestion erreur GET profile réussi");
    }

    // ==================== TESTS PUT /api/v1/profile ====================

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait mettre à jour le profil")
    @WithMockUser
    void updateProfile_WithValidData_ShouldUpdateSuccessfully() throws Exception {
        // Given
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto(
                "John Updated", "john@example.com", "0698765432",
                "456 Street", "Lyon", "Rhone", "69001", "FR");

        ProfileResponseDto responseDto = TestDataBuilder.createProfileResponseDto(
                1L, "John Updated", "john@example.com", "0698765432", null);

        when(profileService.updateProfile(any(ProfileRequestDto.class)))
                .thenReturn(responseDto);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.mobileNumber").value("0698765432"));

        verify(profileService, times(1)).updateProfile(any(ProfileRequestDto.class));
        log.info("✅ Test PUT profile réussi");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait échouer avec données invalides")
    @WithMockUser
    void updateProfile_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Given - Données invalides
        ProfileRequestDto requestDto = new ProfileRequestDto();
        requestDto.setName("");  // Nom vide
        requestDto.setEmail("invalid-email");

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(profileService);
        log.info("✅ Test validation PUT profile réussi");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait échouer sans authentification")
    void updateProfile_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        // Given
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto();
        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(profileService);
        log.info("✅ Test sécurité PUT profile réussi");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait gérer les erreurs du service")
    @WithMockUser
    void updateProfile_WhenServiceFails_ShouldReturnError() throws Exception {
        // Given
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto();
        String requestJson = objectMapper.writeValueAsString(requestDto);

        when(profileService.updateProfile(any(ProfileRequestDto.class)))
                .thenThrow(new RuntimeException("Update failed"));

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(profileService, times(1)).updateProfile(any(ProfileRequestDto.class));
        log.info("✅ Test gestion erreur PUT profile réussi");
    }

    // ==================== TESTS DE VALIDATION ====================

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait valider le nom requis")
    @WithMockUser
    void updateProfile_WithoutName_ShouldReturnBadRequest() throws Exception {
        // Given - Sans nom
        ProfileRequestDto requestDto = new ProfileRequestDto();
        requestDto.setEmail("john@example.com");
        requestDto.setMobileNumber("0612345678");

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(profileService);
        log.info("✅ Validation nom requis vérifiée");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait valider l'email requis")
    @WithMockUser
    void updateProfile_WithoutEmail_ShouldReturnBadRequest() throws Exception {
        // Given - Sans email
        ProfileRequestDto requestDto = new ProfileRequestDto();
        requestDto.setName("John Doe");
        requestDto.setMobileNumber("0612345678");

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(profileService);
        log.info("✅ Validation email requis vérifiée");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait valider le format de l'email")
    @WithMockUser
    void updateProfile_WithInvalidEmailFormat_ShouldReturnBadRequest() throws Exception {
        // Given - Email invalide
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto();
        requestDto.setEmail("not-an-email");

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(profileService);
        log.info("✅ Validation format email vérifiée");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait valider le numéro de téléphone")
    @WithMockUser
    void updateProfile_WithInvalidPhone_ShouldReturnBadRequest() throws Exception {
        // Given - Téléphone invalide
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto();
        requestDto.setMobileNumber("abc123");

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(profileService);
        log.info("✅ Validation téléphone vérifiée");
    }

    // ==================== TESTS DE CONTENU ====================

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait accepter JSON malformé et retourner 400")
    @WithMockUser
    void updateProfile_WithMalformedJson_ShouldReturnBadRequest() throws Exception {
        // Given - JSON invalide
        String malformedJson = "{invalid json}";

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(profileService);
        log.info("✅ Test JSON malformé réussi");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait rejeter Content-Type invalide")
    @WithMockUser
    void updateProfile_WithInvalidContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        // Given
        String content = "invalid content";

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(content))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(profileService);
        log.info("✅ Test Content-Type invalide réussi");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait accepter body vide et retourner 400")
    @WithMockUser
    void updateProfile_WithEmptyBody_ShouldReturnBadRequest() throws Exception {
        // Given - Body vide
        String emptyJson = "{}";

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(profileService);
        log.info("✅ Test body vide réussi");
    }

    // ==================== TESTS AVEC ADRESSE ====================

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait mettre à jour avec une nouvelle adresse")
    @WithMockUser
    void updateProfile_WithNewAddress_ShouldUpdateProfileAndAddress() throws Exception {
        // Given
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto(
                "John Doe",
                "john@example.com",
                "0612345678",
                "123 New Street",
                "Marseille",
                "PACA",
                "13001",
                "FR"
        );

        AddressDto addressDto = new  AddressDto();
        addressDto.setCity("Marseille");
        addressDto.setCountry("FR");
        addressDto.setPostalCode("13001");
        addressDto.setState("PACA");
        addressDto.setStreet("123 New Street");

        ProfileResponseDto responseDto = TestDataBuilder.createProfileResponseDto(
                1L, "John Doe", "john@example.com", "0612345678", addressDto);

        when(profileService.updateProfile(any(ProfileRequestDto.class)))
                .thenReturn(responseDto);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address.street").value("123 New Street"))
                .andExpect(jsonPath("$.address.city").value("Marseille"));

        verify(profileService, times(1)).updateProfile(any(ProfileRequestDto.class));
        log.info("✅ Test mise à jour avec adresse réussi");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait mettre à jour sans adresse")
    @WithMockUser
    void updateProfile_WithoutAddress_ShouldUpdateOnlyProfile() throws Exception {
        // Given - Sans adresse
        ProfileRequestDto requestDto = new ProfileRequestDto();
        requestDto.setName("John No Address");
        requestDto.setEmail("john@example.com");
        requestDto.setMobileNumber("0612345678");

        ProfileResponseDto responseDto = TestDataBuilder.createProfileResponseDto(
                1L, "John No Address", "john@example.com", "0612345678", null);

        when(profileService.updateProfile(any(ProfileRequestDto.class)))
                .thenReturn(responseDto);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John No Address"))
                .andExpect(jsonPath("$.address").doesNotExist());

        verify(profileService, times(1)).updateProfile(any(ProfileRequestDto.class));
        log.info("✅ Test mise à jour sans adresse réussi");
    }

    // ==================== TESTS DE CHANGEMENT D'EMAIL ====================

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait détecter le changement d'email")
    @WithMockUser
    void updateProfile_WithEmailChange_ShouldFlagEmailUpdated() throws Exception {
        // Given
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto(
                "John Doe",
                "john.newemail@example.com",  // Nouvel email
                "0612345678",
                "123 Street",
                "Paris",
                "IDF",
                "75001",
                "FR"
        );

        ProfileResponseDto responseDto = TestDataBuilder.createProfileResponseDto(
                1L, "John Doe", "john.newemail@example.com", "0612345678", null);
        responseDto.setEmailUpdated(true);

        when(profileService.updateProfile(any(ProfileRequestDto.class)))
                .thenReturn(responseDto);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.newemail@example.com"))
                .andExpect(jsonPath("$.emailUpdated").value(true));

        verify(profileService, times(1)).updateProfile(any(ProfileRequestDto.class));
        log.info("✅ Test changement d'email réussi");
    }

    // ==================== TESTS D'INTÉGRATION DES APPELS ====================

    @Test
    @DisplayName("GET /api/v1/profile - Devrait appeler le service une seule fois")
    @WithMockUser
    void getProfile_ShouldCallServiceOnce() throws Exception {
        // Given
        ProfileResponseDto responseDto = TestDataBuilder.createProfileResponseDto(
                1L, "John Doe", "john@example.com", "0612345678", null);

        when(profileService.getProfile()).thenReturn(responseDto);

        // When
        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isOk());

        // Then
        verify(profileService, times(1)).getProfile();
        verify(profileService, only()).getProfile();
        log.info("✅ Vérification appel service réussi");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait appeler le service avec les bonnes données")
    @WithMockUser
    void updateProfile_ShouldCallServiceWithCorrectData() throws Exception {
        // Given
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto(
                "Test Name",
                "test@example.com",
                "0612345678",
                "Test Street",
                "Test City",
                "Test State",
                "12345",
                "FR"
        );

        ProfileResponseDto responseDto = TestDataBuilder.createProfileResponseDto(
                1L, "Test Name", "test@example.com", "0612345678", null);

        when(profileService.updateProfile(any(ProfileRequestDto.class)))
                .thenReturn(responseDto);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then
        verify(profileService, times(1)).updateProfile(any(ProfileRequestDto.class));
        verify(profileService, only()).updateProfile(any(ProfileRequestDto.class));
        log.info("✅ Vérification appel service avec données réussi");
    }

    // ==================== TESTS DE FORMAT DE RÉPONSE ====================

    @Test
    @DisplayName("GET /api/v1/profile - Devrait retourner le bon format JSON")
    @WithMockUser
    void getProfile_ShouldReturnCorrectJsonFormat() throws Exception {
        // Given
        ProfileResponseDto responseDto = TestDataBuilder.createProfileResponseDto(
                1L, "John Doe", "john@example.com", "0612345678", null);

        when(profileService.getProfile()).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/profile"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.mobileNumber").exists())
                .andExpect(jsonPath("$.emailUpdated").exists());

        log.info("✅ Test format JSON réussi");
    }

    @Test
    @DisplayName("PUT /api/v1/profile - Devrait retourner le bon format JSON")
    @WithMockUser
    void updateProfile_ShouldReturnCorrectJsonFormat() throws Exception {
        // Given
        ProfileRequestDto requestDto = TestDataBuilder.createProfileRequestDto();
        ProfileResponseDto responseDto = TestDataBuilder.createProfileResponseDto(
                1L, "John Updated", "john@example.com", "0698765432", null);

        when(profileService.updateProfile(any(ProfileRequestDto.class)))
                .thenReturn(responseDto);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(put("/api/v1/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").isNumber())
                .andExpect(jsonPath("$.name").isString())
                .andExpect(jsonPath("$.email").isString())
                .andExpect(jsonPath("$.mobileNumber").isString())
                .andExpect(jsonPath("$.emailUpdated").isBoolean());

        log.info("✅ Test format JSON réponse réussi");
    }
}