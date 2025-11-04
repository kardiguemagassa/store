import com.store.store.repository.CustomerRepository;
import com.store.store.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

// ============================================================
// 4. TESTS D'INTÉGRATION - StoreNonProdAuthenticationProviderTest.java
// ============================================================
//package com.store.store.security;

import com.store.store.entity.Customer;
import com.store.store.entity.Role;
import com.store.store.repository.CustomerRepository;
import com.store.store.repository.RoleRepository;
import com.store.store.security.CustomerUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests du AuthenticationProvider.
 * Testent le processus d'authentification complet.
 */
/*@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Slf4j
class StoreNonProdAuthenticationProviderTest {

    @Autowired
    private StoreNonProdUsernamePwdAuthenticationProvider authProvider;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "auth-test@example.com";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    @DisplayName("Authentification réussie avec credentials valides")
    void authenticate_WithValidCredentials_ShouldSucceed() {
        // Arrange
        Customer customer = createAuthenticatableCustomer(TEST_EMAIL, TEST_PASSWORD);

        Authentication authRequest = new UsernamePasswordAuthenticationToken(
                TEST_EMAIL,
                TEST_PASSWORD
        );

        // Act
        Authentication result = authProvider.authenticate(authRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertEquals(TEST_EMAIL, result.getName());

        assertInstanceOf(CustomerUserDetails.class, result.getPrincipal());
        CustomerUserDetails userDetails = (CustomerUserDetails) result.getPrincipal();
        assertEquals(TEST_EMAIL, userDetails.getUsername());
        assertEquals(customer.getName(), userDetails.customer().getName());

        log.info("✅ Authentification réussie pour {}", TEST_EMAIL);
    }

    @Test
    @DisplayName("Échec d'authentification avec email inexistant")
    void authenticate_WithNonExistentEmail_ShouldThrowBadCredentials() {
        // Arrange
        Authentication authRequest = new UsernamePasswordAuthenticationToken(
                "nonexistent@example.com",
                TEST_PASSWORD
        );

        // Act & Assert
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authProvider.authenticate(authRequest);
        });

        assertNotNull(exception.getMessage());
        log.info("✅ Authentification correctement rejetée pour email inexistant");
    }

    @Test
    @DisplayName("Échec d'authentification avec mot de passe incorrect")
    void authenticate_WithWrongPassword_ShouldThrowBadCredentials() {
        // Arrange
        createAuthenticatableCustomer(TEST_EMAIL, TEST_PASSWORD);

        Authentication authRequest = new UsernamePasswordAuthenticationToken(
                TEST_EMAIL,
                "wrongPassword"
        );

        // Act & Assert
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authProvider.authenticate(authRequest);
        });

        assertNotNull(exception.getMessage());
        log.info("✅ Authentification correctement rejetée pour mot de passe incorrect");
    }

    @Test
    @DisplayName("Les rôles devraient être correctement mappés vers des autorités")
    void authenticate_ShouldMapRolesToAuthorities() {
        // Arrange
        createAuthenticatableCustomer(TEST_EMAIL, TEST_PASSWORD);

        Authentication authRequest = new UsernamePasswordAuthenticationToken(
                TEST_EMAIL,
                TEST_PASSWORD
        );

        // Act
        Authentication result = authProvider.authenticate(authRequest);

        // Assert
        assertEquals(2, result.getAuthorities().size());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));

        log.info("✅ Rôles correctement mappés: {}", result.getAuthorities());
    }

    @Test
    @DisplayName("CustomerUserDetails devrait contenir toutes les informations du customer")
    void authenticate_ShouldReturnCustomerUserDetailsWithAllProperties() {
        // Arrange
        createAuthenticatableCustomer(TEST_EMAIL, TEST_PASSWORD);

        Authentication authRequest = new UsernamePasswordAuthenticationToken(
                TEST_EMAIL,
                TEST_PASSWORD
        );

        // Act
        Authentication result = authProvider.authenticate(authRequest);
        CustomerUserDetails userDetails = (CustomerUserDetails) result.getPrincipal();

        // Assert
        assertEquals(TEST_EMAIL, userDetails.getUsername());
        assertEquals("Test User", userDetails.customer().getName());
        assertEquals("0123456789", userDetails.customer().getMobileNumber());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());

        log.info("✅ CustomerUserDetails complet avec toutes les propriétés");
    }

    // Méthode utilitaire pour créer un customer authentifiable
    private Customer createAuthenticatableCustomer(String email, String password) {
        Role userRole = createAndSaveRole("ROLE_USER");
        Role adminRole = createAndSaveRole("ROLE_ADMIN");

        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setName("Test User");
        customer.setMobileNumber("0123456789");
        customer.setPasswordHash(passwordEncoder.encode(password));
        customer.setRoles(new HashSet<>(Set.of(userRole, adminRole)));

        return customerRepository.save(customer);
    }

    private Role createAndSaveRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        role.setCreatedBy("TEST");
        return roleRepository.save(role);
    }
}

 */