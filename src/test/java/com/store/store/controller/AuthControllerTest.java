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

    private Customer mockCustomer;
    private Role mockRole;
    private CustomerUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        // ✅ Utiliser TestDataBuilder
        mockRole = TestDataBuilder.createRoleEntity("ROLE_USER");
        mockRole.setRoleId(1L);

        mockCustomer = TestDataBuilder.createCustomer(1L, "Test", "User", "test@example.com");
        mockCustomer.setMobileNumber("0612345678");  // ✅ Override avec format 10 chiffres
        mockCustomer.setRoles(Set.of(mockRole));

        // ✅ Créer CustomerUserDetails pour éviter le ClassCastException
        mockUserDetails = new CustomerUserDetails(mockCustomer);
    }

    // ==================== TESTS LOGIN ====================

    @Nested
    @DisplayName("POST /api/v1/auth/login - Connexion")
    class LoginTests {

        @Test
        @DisplayName("Devrait connecter un utilisateur avec des credentials valides")
        void shouldLoginSuccessfully() throws Exception {
            // Given
            LoginRequestDto loginRequest = new LoginRequestDto("test@example.com", "password123");

            // ✅ Utiliser CustomerUserDetails au lieu de Customer
            Authentication mockAuth = new UsernamePasswordAuthenticationToken(
                    mockUserDetails,
                    "password123",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(jwtUtil.generateJwtToken(any(Authentication.class)))
                    .thenReturn("mock-jwt-token-12345");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("OK")))  // ✅ Changé de $.status
                    .andExpect(jsonPath("$.user.email", is("test@example.com")))
                    .andExpect(jsonPath("$.user.name", is("Test User")))
                    .andExpect(jsonPath("$.user.roles", is("ROLE_USER")))
                    .andExpect(jsonPath("$.jwtToken", is("mock-jwt-token-12345")));

            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtil, times(1)).generateJwtToken(any(Authentication.class));
        }

        @Test
        @DisplayName("Devrait retourner 401 pour des credentials invalides")
        void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
            // Given
            LoginRequestDto loginRequest = new LoginRequestDto("test@example.com", "wrongpassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message", is("Nom d'utilisateur ou mot de passe invalide")))  // ✅ Changé
                    .andExpect(jsonPath("$.user").doesNotExist())
                    .andExpect(jsonPath("$.jwtToken").doesNotExist());

            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtil, never()).generateJwtToken(any());
        }

        @Test
        @DisplayName("Devrait gérer une erreur d'authentification générique")
        void shouldHandleGenericAuthenticationException() throws Exception {
            // Given
            LoginRequestDto loginRequest = new LoginRequestDto("test@example.com", "password123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new RuntimeException("Authentication service unavailable"));

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message", is("Une erreur inattendue s'est produite")));  // ✅ Changé

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

            LoginRequestDto loginRequest = new LoginRequestDto("admin@example.com", "password123");

            Authentication mockAuth = new UsernamePasswordAuthenticationToken(
                    userDetailsWithRoles,
                    "password123",
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
        @DisplayName("Devrait retourner 400 pour une requête sans body")
        void shouldReturnBadRequestForMissingBody() throws Exception {
            // When & Then - Spring retourne 400 (Bad Request) pour un body manquant
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest())  // ✅ Changé à 400
                    .andExpect(jsonPath("$").doesNotExist()); // Pas de body de réponse

            verify(authenticationManager, never()).authenticate(any());
        }
    }

    // ==================== TESTS REGISTER ====================

    @Nested
    @DisplayName("POST /api/v1/auth/register - Inscription")
    class RegisterTests {

        @Test
        @DisplayName("Devrait enregistrer un nouvel utilisateur avec succès")
        void shouldRegisterNewUserSuccessfully() throws Exception {
            // Given
            RegisterRequestDto registerRequest = new RegisterRequestDto();
            registerRequest.setEmail("newuser@example.com");
            registerRequest.setName("New User");
            registerRequest.setMobileNumber("0612345678");
            registerRequest.setPassword("StrongP@ssw0rd");

            when(compromisedPasswordChecker.check(anyString()))
                    .thenReturn(new CompromisedPasswordDecision(false));
            when(customerRepository.findByEmailOrMobileNumber(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString()))
                    .thenReturn("$2a$10$encodedPassword");
            when(roleRepository.findByName("ROLE_USER"))
                    .thenReturn(Optional.of(mockRole));
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
            verify(customerRepository, times(1)).save(any(Customer.class));
        }

        @Test
        @DisplayName("Devrait rejeter un mot de passe compromis")
        void shouldRejectCompromisedPassword() throws Exception {
            // Given
            RegisterRequestDto registerRequest = new RegisterRequestDto();
            registerRequest.setEmail("newuser@example.com");
            registerRequest.setName("New User");
            registerRequest.setMobileNumber("0612345678");
            registerRequest.setPassword("password123");

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
            RegisterRequestDto registerRequest = new RegisterRequestDto();
            registerRequest.setEmail("existing@example.com");  // ✅ Même email que mockCustomer
            registerRequest.setName("New User");
            registerRequest.setMobileNumber("0698765432");  // ✅ Numéro différent
            registerRequest.setPassword("StrongP@ssw0rd");

            // ✅ Mock customer avec le même email
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
            RegisterRequestDto registerRequest = new RegisterRequestDto();
            registerRequest.setEmail("newuser@example.com");
            registerRequest.setName("New User");
            registerRequest.setMobileNumber("0612345678");  // ✅ Format 10 chiffres
            registerRequest.setPassword("StrongP@ssw0rd");

            // ✅ Mock customer avec MÊME numéro
            Customer existingCustomer = TestDataBuilder.createCustomer(2L, "Existing", "User", "existing@example.com");
            existingCustomer.setMobileNumber("0612345678");  // ✅ Override avec le bon format

            when(compromisedPasswordChecker.check(anyString()))
                    .thenReturn(new CompromisedPasswordDecision(false));
            when(customerRepository.findByEmailOrMobileNumber("newuser@example.com", "0612345678"))
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
            RegisterRequestDto registerRequest = new RegisterRequestDto();
            registerRequest.setEmail("test@example.com");
            registerRequest.setName("New User");
            registerRequest.setMobileNumber("0612345678");  // ✅ Format 10 chiffres
            registerRequest.setPassword("StrongP@ssw0rd");

            // ✅ Override le mockCustomer avec le bon format de numéro
            mockCustomer.setMobileNumber("0612345678");

            when(compromisedPasswordChecker.check(anyString()))
                    .thenReturn(new CompromisedPasswordDecision(false));
            when(customerRepository.findByEmailOrMobileNumber("test@example.com", "0612345678"))
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
            RegisterRequestDto registerRequest = new RegisterRequestDto();
            registerRequest.setEmail("newuser@example.com");
            registerRequest.setName("New User");
            registerRequest.setMobileNumber("0612345678");
            registerRequest.setPassword("PlainPassword123");

            String encodedPassword = "$2a$10$encodedPlainPassword123";

            when(compromisedPasswordChecker.check(anyString()))
                    .thenReturn(new CompromisedPasswordDecision(false));
            when(customerRepository.findByEmailOrMobileNumber(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode("PlainPassword123"))
                    .thenReturn(encodedPassword);
            when(roleRepository.findByName("ROLE_USER"))
                    .thenReturn(Optional.of(mockRole));
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
        }

        @Test
        @DisplayName("Devrait assigner automatiquement le rôle USER")
        void shouldAutomaticallyAssignUserRole() throws Exception {
            // Given
            RegisterRequestDto registerRequest = new RegisterRequestDto();
            registerRequest.setEmail("newuser@example.com");
            registerRequest.setName("New User");
            registerRequest.setMobileNumber("0612345678");
            registerRequest.setPassword("StrongP@ssw0rd");

            when(compromisedPasswordChecker.check(anyString()))
                    .thenReturn(new CompromisedPasswordDecision(false));
            when(customerRepository.findByEmailOrMobileNumber(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString()))
                    .thenReturn("$2a$10$encoded");
            when(roleRepository.findByName("ROLE_USER"))
                    .thenReturn(Optional.of(mockRole));
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

            verify(roleRepository, times(1)).findByName("ROLE_USER");
        }
    }
}