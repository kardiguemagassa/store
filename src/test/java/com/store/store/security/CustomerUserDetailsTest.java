// ============================================================
// 2. TESTS UNITAIRES - CustomerUserDetailsTest.java
// ============================================================
package com.store.store.security;

import com.store.store.entity.Customer;
import com.store.store.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du record CustomerUserDetails.
 * Vérifie le mapping Customer → UserDetails.
 */
class CustomerUserDetailsTest {

    @Test
    @DisplayName("Devrait créer CustomerUserDetails avec tous les attributs corrects")
    void shouldCreateCustomerUserDetailsSuccessfully() {
        // Arrange
        Customer customer = createCustomer("test@store.com", "Test User");

        Role userRole = createRole("ROLE_USER");
        Role adminRole = createRole("ROLE_ADMIN");
        customer.setRoles(Set.of(userRole, adminRole));

        // Act
        CustomerUserDetails userDetails = new CustomerUserDetails(customer);

        // Assert
        assertEquals("test@store.com", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertEquals("Test User", userDetails.customer().getName());
        assertEquals("0123456789", userDetails.customer().getMobileNumber());
        assertEquals(2, userDetails.getAuthorities().size());

        // Vérifier les flags UserDetails
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
    }

    @Test
    @DisplayName("Devrait gérer les rôles null ou vides sans erreur")
    void shouldHandleNullOrEmptyRoles() {
        // Arrange - Rôles null
        Customer customerNull = createCustomer("null@store.com", "Null Roles");
        customerNull.setRoles(null);

        // Act
        CustomerUserDetails userDetailsNull = new CustomerUserDetails(customerNull);

        // Assert
        assertNotNull(userDetailsNull.getAuthorities());
        assertTrue(userDetailsNull.getAuthorities().isEmpty());

        // Arrange - Rôles vides
        Customer customerEmpty = createCustomer("empty@store.com", "Empty Roles");
        customerEmpty.setRoles(Collections.emptySet());

        // Act
        CustomerUserDetails userDetailsEmpty = new CustomerUserDetails(customerEmpty);

        // Assert
        assertNotNull(userDetailsEmpty.getAuthorities());
        assertTrue(userDetailsEmpty.getAuthorities().isEmpty());
    }

    @Test
    @DisplayName("Devrait mapper correctement les noms des autorités")
    void shouldReturnCorrectAuthorityNames() {
        // Arrange
        Customer customer = createCustomer("user@store.com", "User");
        Role userRole = createRole("ROLE_USER");
        customer.setRoles(Set.of(userRole));

        // Act
        CustomerUserDetails userDetails = new CustomerUserDetails(customer);

        // Assert
        var authorities = userDetails.getAuthorities();
        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.iterator().next().getAuthority());
    }

    @Test
    @DisplayName("Devrait rejeter un customer null")
    void shouldThrowExceptionForNullCustomer() {
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerUserDetails(null)
        );
    }

    @Test
    @DisplayName("Devrait rejeter un customer avec email null")
    void shouldThrowExceptionForNullEmail() {
        // Arrange
        Customer customer = new Customer();
        customer.setName("Test User");
        customer.setPasswordHash("encodedPassword");
        // Email non défini = null

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerUserDetails(customer)
        );
    }

    @Test
    @DisplayName("Devrait rejeter un customer avec password null")
    void shouldThrowExceptionForNullPassword() {
        // Arrange
        Customer customer = new Customer();
        customer.setName("Test User");
        customer.setEmail("test@store.com");
        // Password non défini = null

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerUserDetails(customer)
        );
    }

    // Méthodes utilitaires
    private Customer createCustomer(String email, String name) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(email);
        customer.setPasswordHash("encodedPassword");
        customer.setMobileNumber("0123456789");
        return customer;
    }

    private Role createRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        return role;
    }
}