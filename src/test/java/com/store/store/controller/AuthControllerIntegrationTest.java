package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.dto.LoginRequestDto;
import com.store.store.dto.RegisterRequestDto;
import com.store.store.entity.Customer;
import com.store.store.entity.Role;
import com.store.store.repository.CustomerRepository;
import com.store.store.repository.RoleRepository;
import com.store.store.util.JwtUtil;
import com.store.store.util.TestDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
@DisplayName("Tests d'Intégration - AuthController")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private Role userRole;
    private Customer existingCustomer;

    @BeforeEach
    void setUp() {
        // Nettoyer la base de données
        customerRepository.deleteAll();
        roleRepository.deleteAll();

        // ✅ Créer le rôle USER avec TestDataBuilder
        userRole = TestDataBuilder.createRoleEntity("ROLE_USER");
        userRole = roleRepository.save(userRole);

        // ✅ Créer un customer existant avec TestDataBuilder
        existingCustomer = TestDataBuilder.createCustomer(null, "Existing", "User", "existing@example.com");
        existingCustomer.setMobileNumber("0612345678");
        existingCustomer.setPasswordHash(passwordEncoder.encode("password123"));
        existingCustomer.setRoles(Set.of(userRole));
        existingCustomer = customerRepository.save(existingCustomer);

        log.info("✅ Données de test créées : User role, Existing customer");
    }

    // ==================== TESTS LOGIN ====================

    @Test
    @DisplayName("Devrait connecter un utilisateur existant et générer un vrai JWT")
    void shouldLoginExistingUserAndGenerateRealJwt() throws Exception {
        // Given
        LoginRequestDto loginRequest = new LoginRequestDto("existing@example.com", "password123");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("OK")))
                .andExpect(jsonPath("$.user.email", is("existing@example.com")))
                .andExpect(jsonPath("$.user.name", is("Existing User")))
                .andExpect(jsonPath("$.user.roles", containsString("ROLE_USER")))
                .andExpect(jsonPath("$.jwtToken", notNullValue()))
                .andExpect(jsonPath("$.jwtToken", not(emptyString())));

        log.info("✅ Login réussi avec JWT valide");
    }

    @Test
    @DisplayName("Devrait rejeter la connexion avec un mauvais mot de passe")
    void shouldRejectLoginWithWrongPassword() throws Exception {
        // Given
        LoginRequestDto loginRequest = new LoginRequestDto("existing@example.com", "wrongpassword");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Nom d'utilisateur ou mot de passe invalide")))
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.jwtToken").doesNotExist());

        log.info("✅ Connexion rejetée pour mauvais mot de passe");
    }

    @Test
    @DisplayName("Devrait rejeter la connexion pour un utilisateur inexistant")
    void shouldRejectLoginForNonExistentUser() throws Exception {
        // Given
        LoginRequestDto loginRequest = new LoginRequestDto("nonexistent@example.com", "password123");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Nom d'utilisateur ou mot de passe invalide")));

        log.info("✅ Connexion rejetée pour utilisateur inexistant");
    }

    // ==================== TESTS REGISTER ====================

    @Test
    @DisplayName("Devrait enregistrer un nouvel utilisateur et le persister en base")
    void shouldRegisterNewUserAndPersistToDatabase() throws Exception {
        // Given
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("New User");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setMobileNumber("0698765432");
        registerRequest.setPassword("Xk9#mP$vL2qR!");  // ✅ Mot de passe plus complexe

        // When
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().string("Inscription réussie"));

        // Then - Vérifier que l'utilisateur est bien en base
        Customer savedCustomer = customerRepository.findByEmail("newuser@example.com").orElseThrow();

        assert savedCustomer.getName().equals("New User");
        assert savedCustomer.getEmail().equals("newuser@example.com");
        assert savedCustomer.getMobileNumber().equals("0698765432");
        assert savedCustomer.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_USER"));

        // Vérifier que le mot de passe est encodé
        assert passwordEncoder.matches("Xk9#mP$vL2qR!", savedCustomer.getPasswordHash());

        log.info("✅ Nouvel utilisateur enregistré et persisté en base");
    }

    @Test
    @DisplayName("Devrait rejeter l'inscription avec un email déjà enregistré")
    void shouldRejectRegistrationWithDuplicateEmail() throws Exception {
        // Given
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("Another User");
        registerRequest.setEmail("existing@example.com");
        registerRequest.setMobileNumber("0698765432");
        registerRequest.setPassword("Yz7&nQ!pW3tK@");  // ✅ Mot de passe plus complexe

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email", is("L'e-mail est déjà enregistré")));

        log.info("✅ Inscription rejetée pour email dupliqué");
    }

    @Test
    @DisplayName("Devrait rejeter l'inscription avec un numéro de téléphone déjà enregistré")
    void shouldRejectRegistrationWithDuplicateMobileNumber() throws Exception {
        // Given
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("Another User");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setMobileNumber("0612345678");
        registerRequest.setPassword("Hf4$bN@mT8vL!");  // ✅ Mot de passe plus complexe

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mobileNumber",
                        is("Le numéro de téléphone portable est déjà enregistré")));

        log.info("✅ Inscription rejetée pour numéro de téléphone dupliqué");
    }

    @Test
    @DisplayName("Devrait rejeter un mot de passe faible")
    void shouldRejectWeakPassword() throws Exception {
        // Given
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("New User");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setMobileNumber("0698765432");
        registerRequest.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password", is("Choisissez un mot de passe fort")));

        log.info("✅ Inscription rejetée pour mot de passe faible");
    }

    @Test
    @DisplayName("Devrait vérifier que le JWT généré est valide et contient les bonnes informations")
    void shouldGenerateValidJwtWithCorrectClaims() throws Exception {
        // Given
        LoginRequestDto loginRequest = new LoginRequestDto("existing@example.com", "password123");

        // When
        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then - Extraire et valider le JWT
        String jwtToken = objectMapper.readTree(responseBody).get("jwtToken").asText();

        // ✅ Utiliser les VRAIES méthodes de JwtUtil
        assert jwtUtil.validateJwtToken(jwtToken) : "Le JWT devrait être valide";

        String emailFromToken = jwtUtil.getEmailFromJwtToken(jwtToken);
        assert emailFromToken.equals("existing@example.com") : "L'email devrait être 'existing@example.com'";

        log.info("✅ JWT valide généré avec les bonnes informations (email: {})", emailFromToken);
    }

    @Test
    @DisplayName("Devrait encoder correctement le mot de passe lors de l'inscription")
    void shouldEncodePasswordDuringRegistration() throws Exception {
        // Given
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("Test User");
        registerRequest.setEmail("testuser@example.com");
        registerRequest.setMobileNumber("0687654321");
        registerRequest.setPassword("MySecureP@ssw0rd!");

        // When
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Then
        Customer savedCustomer = customerRepository.findByEmail("testuser@example.com").orElseThrow();

        // Le hash ne doit PAS être le mot de passe en clair
        assert !savedCustomer.getPasswordHash().equals("MySecureP@ssw0rd!");

        // Le hash doit commencer par $2a$ (BCrypt)
        assert savedCustomer.getPasswordHash().startsWith("$2a$");

        // Le mot de passe doit matcher avec l'encoder
        assert passwordEncoder.matches("MySecureP@ssw0rd!", savedCustomer.getPasswordHash());

        log.info("✅ Mot de passe correctement encodé avec BCrypt");
    }

    @Test
    @DisplayName("Devrait assigner automatiquement le rôle USER lors de l'inscription")
    void shouldAutoAssignUserRoleDuringRegistration() throws Exception {
        // Given
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setName("Role Test User");
        registerRequest.setEmail("roletest@example.com");
        registerRequest.setMobileNumber("0676543210");
        registerRequest.setPassword("Gr5#kV$xM9qZ!");  // ✅ Mot de passe plus complexe

        // When
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Then
        Customer savedCustomer = customerRepository.findByEmail("roletest@example.com").orElseThrow();

        assert savedCustomer.getRoles().size() == 1;
        assert savedCustomer.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_USER"));

        log.info("✅ Rôle USER automatiquement assigné");
    }

    @Test
    @DisplayName("Devrait vérifier que le JWT contient tous les claims nécessaires")
    void shouldContainAllRequiredClaimsInJwt() throws Exception {
        // Given
        LoginRequestDto loginRequest = new LoginRequestDto("existing@example.com", "password123");

        // When
        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then
        String jwtToken = objectMapper.readTree(responseBody).get("jwtToken").asText();

        // Valider le token
        assert jwtUtil.validateJwtToken(jwtToken);

        // Extraire l'email
        String email = jwtUtil.getEmailFromJwtToken(jwtToken);
        assert email.equals("existing@example.com");

        // Le token devrait contenir les informations de base
        assert jwtToken.split("\\.").length == 3 : "Le JWT devrait avoir 3 parties (header.payload.signature)";

        log.info("✅ JWT contient tous les claims requis et est structuré correctement");
    }
}