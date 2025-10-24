package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests Unitaires pour DummyController
 * Couvre tous les endpoints avec différentes stratégies de test
 */
@WebMvcTest(
        controllers = DummyController.class,
        excludeAutoConfiguration = org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
)
@Slf4j
@DisplayName("Tests Unitaires - DummyController")
class DummyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto validUserDto;
    private static final String BASE_URL = "/api/v1/dummy";

    @BeforeEach
    void setUp() {
        validUserDto = new UserDto();
        validUserDto.setUserId(1L);
        validUserDto.setName("John");
        validUserDto.setEmail("john.doe@example.com");
    }

    // CREATE USER
    @Test
    @DisplayName("POST /create-user - Devrait créer un utilisateur avec succès")
    void createUser_WithValidUser_ShouldReturnSuccess() throws Exception {
        // Given
        String userJson = objectMapper.writeValueAsString(validUserDto);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/create-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("User created successfully"));

        log.info("User created successfully");
    }

    @Test
    @DisplayName("POST /create-user - Devrait gérer un utilisateur avec des champs null")
    void createUser_WithNullUser_ShouldReturnSuccess() throws Exception {
        // When & Then - Utiliser un objet JSON vide
        mockMvc.perform(post(BASE_URL + "/create-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")) // Objet vide plutôt que "null"
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("User created successfully"));
    }

    @Test
    @DisplayName("POST /create-user - Devrait échouer avec un Content-Type invalide")
    void createUser_WithInvalidContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        // Given
        String userJson = objectMapper.writeValueAsString(validUserDto);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/create-user")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(userJson))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());
    }

    // REQUEST ENTITY
    @Test
    @DisplayName("POST /request-entity - Devrait traiter RequestEntity avec succès")
    void createUserWithEntity_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        String userJson = objectMapper.writeValueAsString(validUserDto);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/request-entity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson)
                        .header("Custom-Header", "custom-value"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("User created successfully"));
    }

    //HEADERS
    @Test
    @DisplayName("GET /headers - Devrait lire les headers correctement")
    void readHeaders_WithValidHeaders_ShouldReturnSuccess() throws Exception {
        // Given
        String userLocation = "Paris, France";

        // When & Then
        mockMvc.perform(get(BASE_URL + "/headers")
                        .header("User-Location", userLocation)
                        .header("User-Agent", "Mozilla/5.0"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Recevied headers with value")));
        log.info("Headers : {}", mockMvc.perform(get(BASE_URL + "/headers")));
    }

    @Test
    @DisplayName("GET /headers - Devrait gérer l'absence du header User-Location")
    void readHeaders_WithoutUserLocation_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL + "/headers")
                        .header("Other-Header", "value"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    //SEARCH WITH VALIDATION
    @ParameterizedTest
    @ValueSource(strings = {"validName", "longerValidUserName", "exactlyThirtyCharsLong!"}) // 30 chars max
    @DisplayName("GET /search - Devrait accepter les noms valides")
    void searchUser_WithValidNameLength_ShouldReturnSuccess(String userName) throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL + "/search")
                        .param("name", userName))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Searching for user : " + userName)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    // Retirer les espaces vides - ils déclenchent la validation @Size
    @DisplayName("GET /search - Devrait utiliser la valeur par défaut pour les noms vides")
    void searchUser_WithEmptyOrNullName_ShouldUseDefaultValue(String userName) throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL + "/search")
                        .param("name", userName))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Searching for user : Guest")));

        log.info("Searching for user : Guest");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "ab", "abc", "abcd"})
    @DisplayName("GET /search - Devrait rejeter les noms trop courts")
    void searchUser_WithTooShortName_ShouldReturnBadRequest(String userName) throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL + "/search")
                        .param("name", userName))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /search - Devrait rejeter les noms trop longs")
    void searchUser_WithTooLongName_ShouldReturnBadRequest() throws Exception {
        // Given
        String longName = "ThisNameIsWayTooLongAndExceedsTheMaximumAllowedLength";

        // When & Then
        mockMvc.perform(get(BASE_URL + "/search")
                        .param("name", longName))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    //MULTIPLE SEARCH
    @Test
    @DisplayName("GET /multiple-search - Devrait gérer multiple paramètres")
    void multipleSearch_WithMultipleParams_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL + "/multiple-search")
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("age", "30"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Searching for user : John Doe"));
    }

    @Test
    @DisplayName("GET /multiple-search - Devrait gérer les paramètres manquants")
    void multipleSearch_WithMissingParams_ShouldHandleNulls() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL + "/multiple-search")
                        .param("firstName", "John"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Searching for user : John null"));
    }

    @Test
    @DisplayName("GET /multiple-search - Devrait gérer l'absence de paramètres")
    void multipleSearch_WithoutParams_ShouldHandleEmptyMap() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL + "/multiple-search"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Searching for user : null null"));
    }

    // PATH VARIABLES
    @ParameterizedTest
    @CsvSource({"123, 456, /user/123/posts/456", "789, null, /user/789"})
    @DisplayName("GET /user/{userId}/posts/{postId} - Devrait gérer différents patterns d'URL")
    void getUser_WithDifferentPathPatterns_ShouldReturnSuccess(String userId, String postId, String url) throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL + url))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Searching for user : " + userId)));
    }

    @Test
    @DisplayName("GET /user/{userId} - Devrait gérer l'absence de postId")
    void getUser_WithoutPostId_ShouldHandleNullPostId() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL + "/user/123"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Searching for user : 123 and post : null"));
    }

    //PATH VARIABLES WITH MAP
    @Test
    @DisplayName("GET /user/map/{userId}/posts/{postId} - Devrait extraire les variables de chemin via Map")
    void getUserUsingMap_WithBothVariables_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL + "/user/map/123/posts/456"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Searching for user : 123 and post : 456"));
    }

    @Test
    @DisplayName("GET /user/map/{userId} - Devrait gérer les variables manquantes dans la Map")
    void getUserUsingMap_WithoutPostId_ShouldHandleMissingVariable() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL + "/user/map/123"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Searching for user : 123 and post : null"));
    }

    //PERFORMANCE ET COMPORTEMENT
    @Test
    @DisplayName("GET /search - Devrait répondre rapidement")
    void searchUser_ShouldRespondQuickly() throws Exception {
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get(BASE_URL + "/search")
                        .param("name", "QuickTest"))
                .andExpect(status().isOk());

        long duration = System.currentTimeMillis() - startTime;

        // Utilisation d'une assertion JUnit appropriée
        org.junit.jupiter.api.Assertions.assertTrue(
                duration < 1000,
                "Response took too long: " + duration + "ms"
        );
    }

    @Test
    @DisplayName("POST /create-user - Devrait avoir le bon Content-Type dans la réponse")
    void createUser_ShouldReturnCorrectContentType() throws Exception {
        String userJson = objectMapper.writeValueAsString(validUserDto);

        mockMvc.perform(post(BASE_URL + "/create-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    // ERREURS
    @Test
    @DisplayName("GET /nonexistent - Devrait retourner 404 pour endpoint inexistant")
    void nonExistentEndpoint_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/nonexistent"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /search - Devrait retourner 405 pour méthode non supportée")
    void searchUser_WithPostMethod_ShouldReturnMethodNotAllowed() throws Exception {
        mockMvc.perform(post(BASE_URL + "/search")
                        .param("name", "test"))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }
}