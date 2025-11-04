package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.dto.LoginRequestDto;
import com.store.store.dto.RegisterRequestDto;
import com.store.store.entity.Customer;
import com.store.store.entity.Role;
import com.store.store.enums.RoleType;
import com.store.store.repository.CustomerRepository;
import com.store.store.repository.RoleRepository;
import com.store.store.util.JwtUtil;
import com.store.store.util.TestDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour AuthController
 * Ces tests utilisent une base de données H2 en mémoire et Spring Security complet
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Slf4j
@DisplayName("Tests d'Intégration - Flux d'Authentification")
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private Role userRole;

    private static final String STRONG_TEST_PASSWORD = "V3ry$tr0ngP@ss1!"; // 16 caractères
    private static final String ANOTHER_STRONG_PASSWORD = "M@sterP@ss123!"; // 14 caractères

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
        roleRepository.deleteAll();

        //userRole = TestDataBuilder.createRoleEntity("ROLE_USER");
        userRole = roleRepository.save(userRole);
    }

    // SCÉNARIOS MÉTIER PRINCIPAUX
    @Nested
    @DisplayName("Scénario complet d'inscription et connexion")
    class CompleteRegistrationLoginFlow {

        @Test
        @DisplayName("Devrait permettre l'inscription, la persistance et la connexion d'un nouvel utilisateur")
        void shouldRegisterPersistAndLoginNewUser() throws Exception {
            // Phase 1: INSCRIPTION
            RegisterRequestDto registerRequest = new RegisterRequestDto();
            registerRequest.setName("John Doe");
            registerRequest.setEmail("john.doe@example.com");
            registerRequest.setMobileNumber("0612345678");
            registerRequest.setPassword(STRONG_TEST_PASSWORD); // ← MOT DE PASSE FORT

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string("Inscription réussie"));

            // Vérification en base de données
            Customer savedCustomer = customerRepository.findByEmail("john.doe@example.com")
                    .orElseThrow(() -> new AssertionError("Utilisateur devrait être persisté"));

            assertThat(savedCustomer.getName()).isEqualTo("John Doe");
            assertThat(savedCustomer.getRoles()).hasSize(1);
            assertThat(passwordEncoder.matches(STRONG_TEST_PASSWORD, savedCustomer.getPasswordHash())).isTrue();

            // Phase 2: CONNEXION
            LoginRequestDto loginRequest = new LoginRequestDto("john.doe@example.com", STRONG_TEST_PASSWORD);

            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jwtToken").exists())
                    .andReturn();

            // Phase 3: VALIDATION JWT
            String responseBody = loginResult.getResponse().getContentAsString();
            String token = objectMapper.readTree(responseBody).get("jwtToken").asText();

            assertThat(jwtUtil.validateJwtToken(token)).isTrue();
            assertThat(jwtUtil.getEmailFromJwtToken(token)).isEqualTo("john.doe@example.com");

            log.info("Scénario complet: inscription -> persistance -> connexion -> JWT valide");
        }
    }

    @Nested
    @DisplayName("Scénarios de sécurité d'authentification")
    class AuthenticationSecurityScenarios {

        private Customer existingUser;

        @BeforeEach
        void setUpSecurityScenario() {
            // Créer un utilisateur existant pour les tests de sécurité
            existingUser = TestDataBuilder.createCustomer(null, "Existing", "User", "security@example.com");
            existingUser.setMobileNumber("0698765432");
            existingUser.setPasswordHash(passwordEncoder.encode(STRONG_TEST_PASSWORD));
            existingUser.setRoles(Set.of(userRole));
            customerRepository.save(existingUser);
        }

        @Test
        @DisplayName("Devrait rejeter les credentials d'authentification invalides")
        void shouldRejectInvalidCredentials() throws Exception {
            // Tests 401 Unauthorized - Authentification
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequestDto("security@example.com", "wrongpassword"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Nom d'utilisateur ou mot de passe invalide"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequestDto("nonexistent@example.com", "anypassword"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Nom d'utilisateur ou mot de passe invalide"));
        }

        @Test
        @DisplayName("Devrait rejeter les requêtes de login avec validation échouée")
        void shouldRejectLoginRequestsWithFailedValidation() throws Exception {
            // Tests 400 Bad Request - Validation
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\": \"invalid-email\", \"password\": \"anypassword\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.username").exists());
        }

        @Test
        @DisplayName("Devrait générer des JWTs valides et distincts pour chaque connexion")
        void shouldGenerateValidDistinctJwtsForEachLogin() throws Exception {
            LoginRequestDto loginRequest = new LoginRequestDto("security@example.com", STRONG_TEST_PASSWORD);

            // Première connexion
            MvcResult result1 = mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jwtToken").exists())
                    .andReturn();

            String jwt1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                    .get("jwtToken").asText();

            // Délai pour garantir des JWTs différents
            Thread.sleep(1000);

            // Seconde connexion
            MvcResult result2 = mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jwtToken").exists())
                    .andReturn();

            String jwt2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                    .get("jwtToken").asText();

            // Vérifications
            assertThat(jwtUtil.validateJwtToken(jwt1)).isTrue();
            assertThat(jwtUtil.validateJwtToken(jwt2)).isTrue();
            assertThat(jwt1).isNotEqualTo(jwt2);
            assertThat(jwtUtil.getEmailFromJwtToken(jwt1)).isEqualTo("security@example.com");
            assertThat(jwtUtil.getEmailFromJwtToken(jwt2)).isEqualTo("security@example.com");

            log.info("JWTs valides et distincts générés pour chaque connexion");
        }
    }

    @Nested
    @DisplayName("Scénarios de validation d'inscription")
    class RegistrationValidationScenarios {

        @Test
        @DisplayName("Devrait rejeter les inscriptions avec des données dupliquées ou invalides")
        void shouldRejectInvalidOrDuplicateRegistrations() throws Exception {
            // Créer un utilisateur existant pour tester les doublons
            Customer existing = TestDataBuilder.createCustomer(null, "Existing", "User", "duplicate@example.com");
            existing.setMobileNumber("0612345678");
            existing.setPasswordHash(passwordEncoder.encode(STRONG_TEST_PASSWORD));
            existing.setRoles(Set.of(userRole));
            customerRepository.save(existing);

            // Tentative 1: Email dupliqué
            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRegisterRequest(
                                    "New User", "duplicate@example.com", "0698765432", ANOTHER_STRONG_PASSWORD))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email").value("L'e-mail est déjà enregistré"));

            // Tentative 2: Téléphone dupliqué
            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRegisterRequest(
                                    "New User", "new@example.com", "0612345678", ANOTHER_STRONG_PASSWORD))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mobileNumber").value("Le numéro de téléphone portable est déjà enregistré"));

            // Tentative 3: Mot de passe faible
            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRegisterRequest(
                                    "New User", "weakpass@example.com", "0634567890", "weak"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.password").exists());

            log.info("Toutes les validations d'inscription fonctionnent correctement");
        }

        @Test
        @DisplayName("Devrait encoder correctement les mots de passe et assigner les rôles")
        void shouldEncodePasswordsAndAssignRolesCorrectly() throws Exception {
            RegisterRequestDto request = createRegisterRequest(
                    "Test User", "encoder@example.com", "0678901234", STRONG_TEST_PASSWORD);

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            Customer saved = customerRepository.findByEmail("encoder@example.com")
                    .orElseThrow();

            // Vérification de l'encodage
            assertThat(saved.getPasswordHash()).startsWith("$2a$");
            assertThat(passwordEncoder.matches(STRONG_TEST_PASSWORD, saved.getPasswordHash())).isTrue();
            assertThat(saved.getPasswordHash()).isNotEqualTo(STRONG_TEST_PASSWORD);

            // Vérification du rôle
            assertThat(saved.getRoles())
                    .extracting(Role::getName)
                    .containsExactly(RoleType.ROLE_USER);

            log.info("Mot de passe encodé et rôle assigné correctement");
        }

        @Test
        @DisplayName("Devrait rejeter les mots de passe compromis avec 400")
        void shouldRejectCompromisedPasswords() throws Exception {
            RegisterRequestDto request = createRegisterRequest(
                    "Test User", "compromised@example.com", "0634567890", "password123");

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.password").exists());
        }
    }

    @Nested
    @DisplayName("Scénarios de validation d'entrée")
    class InputValidationScenarios {

        @Test
        @DisplayName("Devrait rejeter les emails invalides avec 400")
        void shouldRejectInvalidEmails() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequestDto("invalid-email", "anypassword"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.username").exists());
        }

        @Test
        @DisplayName("Devrait rejeter les champs manquants avec 400")
        void shouldRejectMissingFields() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.username").exists())
                    .andExpect(jsonPath("$.errors.password").exists());
        }
    }

    @Nested
    @DisplayName("Scénarios d'authentification")
    class AuthenticationScenarios {

        @Test
        @DisplayName("Devrait rejeter les mauvais mots de passe avec 401")
        void shouldRejectWrongPasswords() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequestDto("security@example.com", "wrongpassword"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Devrait rejeter les utilisateurs inexistants avec 401")
        void shouldRejectNonExistentUsers() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequestDto("ghost@example.com", "anypassword"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
        }
    }


    // MÉTHODES UTILITAIRES
    private RegisterRequestDto createRegisterRequest(String name, String email, String mobile, String password) {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setName(name);
        request.setEmail(email);
        request.setMobileNumber(mobile);
        request.setPassword(password);
        return request;
    }
}