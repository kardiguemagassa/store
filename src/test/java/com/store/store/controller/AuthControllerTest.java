package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.config.TestSecurityConfig;
import com.store.store.dto.LoginRequestDto;
import com.store.store.dto.RegisterRequestDto;
import com.store.store.entity.Customer;
import com.store.store.entity.Role;
import com.store.store.repository.CustomerRepository;
import com.store.store.repository.RoleRepository;
import com.store.store.security.CustomerUserDetails;
import com.store.store.service.IRoleAssignmentService;
import com.store.store.util.JwtUtil;
import com.store.store.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires du AuthController
 */
@WebMvcTest(controllers = AuthController.class)
@Import({TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("Tests Unitaires - AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CompromisedPasswordChecker compromisedPasswordChecker;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private IRoleAssignmentService roleAssignmentService;

    //Constantes
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_MOBILE = "0612345678";
    private static final String TEST_NAME = "Test User";
    private static final String MOCK_JWT = "mock-jwt-token-12345";
    private static final String STRONG_PASSWORD = "StrongP@ssw0rd";

    private Customer mockCustomer;
    private Role mockRole;
    private CustomerUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        // Utiliser TestDataBuilder
        mockRole = TestDataBuilder.createRoleEntity("ROLE_USER");
        mockRole.setRoleId(1L);

        mockCustomer = TestDataBuilder.createCustomer(1L, "Test", "User", TEST_EMAIL);
        mockCustomer.setMobileNumber(TEST_MOBILE);  // format 10 chiffres
        mockCustomer.setRoles(Set.of(mockRole));

        // Créer CustomerUserDetails pour éviter le ClassCastException
        mockUserDetails = new CustomerUserDetails(mockCustomer);
    }

    // HELPERS
    private LoginRequestDto createLoginRequest(String email, String password) {
        return new LoginRequestDto(email, password);
    }

    private RegisterRequestDto createRegisterRequest(String email, String name, String mobile, String password) {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail(email);
        request.setName(name);
        request.setMobileNumber(mobile);
        request.setPassword(password);
        return request;
    }

    private Authentication createMockAuthentication(CustomerUserDetails userDetails) {
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                TEST_PASSWORD,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // TESTS LOGIN
    @Nested
    @DisplayName("POST /api/v1/auth/login - Connexion")
    class LoginTests {

        @Test
        @DisplayName("Devrait connecter un utilisateur avec des credentials valides")
        void shouldLoginSuccessfully() throws Exception {
            // Given
            LoginRequestDto loginRequest = createLoginRequest(TEST_EMAIL, TEST_PASSWORD);
            Authentication mockAuth = createMockAuthentication(mockUserDetails);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(jwtUtil.generateJwtToken(any(Authentication.class)))
                    .thenReturn(MOCK_JWT);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("OK")))
                    .andExpect(jsonPath("$.user.email", is(TEST_EMAIL)))
                    .andExpect(jsonPath("$.user.name", is(TEST_NAME)))
                    .andExpect(jsonPath("$.user.roles", is("ROLE_USER")))
                    .andExpect(jsonPath("$.jwtToken", is(MOCK_JWT)));

            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtil, times(1)).generateJwtToken(any(Authentication.class));
        }

        @Test
        @DisplayName("Devrait retourner 401 pour des credentials invalides")
        void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
            // Given
            LoginRequestDto loginRequest = createLoginRequest(TEST_EMAIL, "wrongpassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When & Then -Vérifiez la structure de GlobalExceptionHandler
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message", is("Nom d'utilisateur ou mot de passe invalide")))
                    .andExpect(jsonPath("$.status", is("Unauthorized")))
                    .andExpect(jsonPath("$.statusCode", is(401)));

            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtil, never()).generateJwtToken(any());
        }

        @Test
        @DisplayName("Devrait gérer une erreur d'authentification générique")
        void shouldHandleGenericAuthenticationException() throws Exception {
            // Given
            LoginRequestDto loginRequest = createLoginRequest(TEST_EMAIL, TEST_PASSWORD);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new RuntimeException("Authentication service unavailable"));

            // When & Then - CORRECTION : Vérifiez la structure de GlobalExceptionHandler
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message", is("Une erreur technique s'est produite")))
                    .andExpect(jsonPath("$.status", is("Internal Server Error")))
                    .andExpect(jsonPath("$.statusCode", is(500)));

            verify(authenticationManager, times(1)).authenticate(any());
        }

        @Test
        @DisplayName("Devrait inclure les rôles multiples dans la réponse")
        void shouldIncludeMultipleRolesInResponse() throws Exception {
            // Given
            Role adminRole = TestDataBuilder.createRoleEntity("ROLE_ADMIN");
            adminRole.setRoleId(2L);
            mockCustomer.setRoles(Set.of(mockRole, adminRole));

            CustomerUserDetails userDetailsWithRoles = new CustomerUserDetails(mockCustomer);

            LoginRequestDto loginRequest = createLoginRequest("admin@example.com", TEST_PASSWORD);

            Authentication mockAuth = new UsernamePasswordAuthenticationToken(
                    userDetailsWithRoles,
                    TEST_PASSWORD,
                    Set.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
            );

            when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
            when(jwtUtil.generateJwtToken(any())).thenReturn("jwt-token");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.roles", containsString("ROLE_")));
        }

        @Test
        @DisplayName("Devrait retourner 400 pour une requête avec body vide")
        void shouldReturnBadRequestForEmptyBody() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.username").exists())
                    .andExpect(jsonPath("$.errors.password").exists());

            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("Devrait retourner 400 pour une requête avec body invalide")
        void shouldReturnBadRequestForInvalidBody() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid-json}"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("JSON")));

            verify(authenticationManager, never()).authenticate(any());
        }
    }

    // TESTS REGISTER
    @Nested
    @DisplayName("POST /api/v1/auth/register - Inscription")
    class RegisterTests {

        @Test
        @DisplayName("Devrait enregistrer un nouvel utilisateur avec succès")
        void shouldRegisterNewUserSuccessfully() throws Exception {
            // Given
            RegisterRequestDto registerRequest = createRegisterRequest(
                    "newuser@example.com", "New User", TEST_MOBILE, STRONG_PASSWORD);

            when(compromisedPasswordChecker.check(anyString()))
                    .thenReturn(new CompromisedPasswordDecision(false));
            when(customerRepository.findByEmailOrMobileNumber(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString()))
                    .thenReturn("$2a$10$encodedPassword");

            // MOCK DU SERVICE DE RÔLES
            Set<Role> userRoles = Set.of(mockRole);
            when(roleAssignmentService.determineInitialRoles(any(RegisterRequestDto.class)))
                    .thenReturn(userRoles);

            when(customerRepository.save(any(Customer.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(content().string("Inscription réussie"));

            verify(compromisedPasswordChecker, times(1)).check(anyString());
            verify(customerRepository, times(1)).findByEmailOrMobileNumber(anyString(), anyString());
            verify(passwordEncoder, times(1)).encode(anyString());
            verify(roleAssignmentService, times(1)).determineInitialRoles(any(RegisterRequestDto.class));
            verify(customerRepository, times(1)).save(any(Customer.class));
        }

        @Test
        @DisplayName("Devrait rejeter un mot de passe compromis")
        void shouldRejectCompromisedPassword() throws Exception {
            // Given
            RegisterRequestDto registerRequest = createRegisterRequest(
                    "newuser@example.com", "New User", TEST_MOBILE, "password123");

            when(compromisedPasswordChecker.check(anyString()))
                    .thenReturn(new CompromisedPasswordDecision(true));

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.password", is("Choisissez un mot de passe fort")));

            verify(compromisedPasswordChecker, times(1)).check(anyString());
            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait rejeter un email déjà enregistré")
        void shouldRejectDuplicateEmail() throws Exception {
            // Given
            RegisterRequestDto registerRequest = createRegisterRequest(
                    "existing@example.com", "New User", "0698765432", STRONG_PASSWORD);

            // Mock customer avec le même email
            Customer existingCustomer = TestDataBuilder.createCustomer(2L, "Existing", "User", "existing@example.com");

            when(compromisedPasswordChecker.check(anyString()))
                    .thenReturn(new CompromisedPasswordDecision(false));
            when(customerRepository.findByEmailOrMobileNumber("existing@example.com", "0698765432"))
                    .thenReturn(Optional.of(existingCustomer));

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email", is("L'e-mail est déjà enregistré")));

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait rejeter un numéro de téléphone déjà enregistré")
        void shouldRejectDuplicateMobileNumber() throws Exception {
            // Given
            RegisterRequestDto registerRequest = createRegisterRequest(
                    "newuser@example.com", "New User", TEST_MOBILE, STRONG_PASSWORD);

            // Mock customer avec meme numéro
            Customer existingCustomer = TestDataBuilder.createCustomer(2L, "Existing", "User", "existing@example.com");
            existingCustomer.setMobileNumber(TEST_MOBILE);  // Override avec le bon format

            when(compromisedPasswordChecker.check(anyString()))
                    .thenReturn(new CompromisedPasswordDecision(false));
            when(customerRepository.findByEmailOrMobileNumber("newuser@example.com", TEST_MOBILE))
                    .thenReturn(Optional.of(existingCustomer));

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mobileNumber",
                            is("Le numéro de téléphone portable est déjà enregistré")));

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait rejeter les deux (email et téléphone) s'ils existent déjà")
        void shouldRejectBothEmailAndMobileNumber() throws Exception {
            // Given
            RegisterRequestDto registerRequest = createRegisterRequest(
                    TEST_EMAIL, "New User", TEST_MOBILE, STRONG_PASSWORD);

            // Override le mockCustomer avec le bon format de numéro
            mockCustomer.setMobileNumber(TEST_MOBILE);

            when(compromisedPasswordChecker.check(anyString()))
                    .thenReturn(new CompromisedPasswordDecision(false));
            when(customerRepository.findByEmailOrMobileNumber(TEST_EMAIL, TEST_MOBILE))
                    .thenReturn(Optional.of(mockCustomer));

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email", is("L'e-mail est déjà enregistré")))
                    .andExpect(jsonPath("$.mobileNumber",
                            is("Le numéro de téléphone portable est déjà enregistré")));

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait valider les champs requis")
        void shouldValidateRequiredFields() throws Exception {
            // Given - Requête avec champs manquants
            RegisterRequestDto registerRequest = new RegisterRequestDto();

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait encoder le mot de passe avant de sauvegarder")
        void shouldEncodePasswordBeforeSaving() throws Exception {
            // Given
            RegisterRequestDto registerRequest = createRegisterRequest(
                    "newuser@example.com", "New User", TEST_MOBILE, "PlainPassword123");

            String encodedPassword = "$2a$10$encodedPlainPassword123";

            when(compromisedPasswordChecker.check(anyString()))
                    .thenReturn(new CompromisedPasswordDecision(false));
            when(customerRepository.findByEmailOrMobileNumber(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode("PlainPassword123"))
                    .thenReturn(encodedPassword);

            // MOCK DU SERVICE DE RÔLES
            Set<Role> userRoles = Set.of(mockRole);
            when(roleAssignmentService.determineInitialRoles(any(RegisterRequestDto.class)))
                    .thenReturn(userRoles);

            when(customerRepository.save(any(Customer.class)))
                    .thenAnswer(invocation -> {
                        Customer saved = invocation.getArgument(0);
                        assert saved.getPasswordHash().equals(encodedPassword);
                        return saved;
                    });

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated());

            verify(passwordEncoder, times(1)).encode("PlainPassword123");
            verify(roleAssignmentService, times(1)).determineInitialRoles(any(RegisterRequestDto.class));
        }

        @Test
        @DisplayName("Devrait assigner automatiquement le rôle USER")
        void shouldAutomaticallyAssignUserRole() throws Exception {
            // Given
            RegisterRequestDto registerRequest = createRegisterRequest(
                    "newuser@example.com", "New User", TEST_MOBILE, STRONG_PASSWORD);

            when(compromisedPasswordChecker.check(anyString()))
                    .thenReturn(new CompromisedPasswordDecision(false));
            when(customerRepository.findByEmailOrMobileNumber(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString()))
                    .thenReturn("$2a$10$encoded");

            // MOCK DU SERVICE DE RÔLES
            Set<Role> userRoles = Set.of(mockRole);
            when(roleAssignmentService.determineInitialRoles(any(RegisterRequestDto.class)))
                    .thenReturn(userRoles);

            when(customerRepository.save(any(Customer.class)))
                    .thenAnswer(invocation -> {
                        Customer saved = invocation.getArgument(0);
                        assert saved.getRoles().contains(mockRole);
                        return saved;
                    });

            // When & Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated());

            verify(roleAssignmentService, times(1)).determineInitialRoles(any(RegisterRequestDto.class));
        }

        @Test
        @DisplayName("Devrait gérer l'absence du rôle USER en base")
        void shouldHandleMissingUserRole() throws Exception {
            // Given
            RegisterRequestDto registerRequest = createRegisterRequest(
                    "newuser@example.com", "New User", TEST_MOBILE, STRONG_PASSWORD);

            when(compromisedPasswordChecker.check(anyString()))
                    .thenReturn(new CompromisedPasswordDecision(false));
            when(customerRepository.findByEmailOrMobileNumber(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            // MOCK DU SERVICE QUI LANCE UNE EXCEPTION
            when(roleAssignmentService.determineInitialRoles(any(RegisterRequestDto.class)))
                    .thenThrow(new RuntimeException("Erreur de configuration du système"));

            // When & Then - CORRECTION : Vérifiez la structure de GlobalExceptionHandler
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message", containsString("technique"))) // Vérifiez le message
                    .andExpect(jsonPath("$.status", is("Internal Server Error"))) // Vérifiez le statut
                    .andExpect(jsonPath("$.statusCode", is(500))); // Vérifiez le code

            verify(customerRepository, never()).save(any());
            verify(roleAssignmentService, times(1)).determineInitialRoles(any(RegisterRequestDto.class));
        }
    }
}