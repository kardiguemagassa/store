/*package com.store.store.util;

import com.store.store.constants.ApplicationConstants;
import com.store.store.entity.Customer;
import com.store.store.entity.Role;
import com.store.store.security.CustomerUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitaires - JwtUtil")
class JwtUtilTest {

    @Mock
    private Environment environment;

    @Mock
    private Authentication authentication;

    private JwtUtil jwtUtil;
    private SecretKey secretKey;
    private Customer customer;
    private CustomerUserDetails customerUserDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(environment);

        String secret = "mySuperSecretKeyThatIsLongEnoughForHS512";
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        Role userRole = new Role();
        userRole.setRoleId(1L);
        userRole.setName("ROLE_USER");

        customer = new Customer();
        customer.setCustomerId(1L);
        customer.setName("John Doe");
        customer.setEmail("john.doe@example.com");
        customer.setMobileNumber("0612345678");
        customer.setPasswordHash("$2a$10$encodedPasswordHash");
        customer.setRoles(Set.of(userRole));

        // CustomerUserDetails avec le Customer
        customerUserDetails = new CustomerUserDetails(customer);
    }

    @Test
    @DisplayName("Devrait générer un token JWT valide avec authentication valide")
    void generateJwtToken_WithValidAuthentication_ShouldReturnValidToken() {
        // Arrange
        when(environment.getProperty(ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE))
                .thenReturn("mySuperSecretKeyThatIsLongEnoughForHS512");

        // Retourner CustomerUserDetails
        when(authentication.getPrincipal()).thenReturn(customerUserDetails);

        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        String token = jwtUtil.generateJwtToken(authentication);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Vérifier que le token peut être parsé
        Claims claims = Jwts.parser().verifyWith(secretKey)
                .build().parseSignedClaims(token).getPayload();

        assertEquals("John Doe", claims.get("username"));
        assertEquals("john.doe@example.com", claims.get("email"));
        assertEquals("0612345678", claims.get("mobileNumber"));
        assertEquals("ROLE_USER,ROLE_ADMIN", claims.get("roles"));
        assertEquals("Eazy Store", claims.getIssuer());
        assertEquals("JWT Token", claims.getSubject());

        verify(environment).getProperty(ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE);
    }

    @Test
    @DisplayName("Devrait générer un token valide avec le secret par défaut")
    void generateJwtToken_WithDefaultSecret_ShouldReturnValidToken() {
        // Arrange
        when(environment.getProperty(ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE))
                .thenReturn(ApplicationConstants.JWT_SECRET_DEFAULT_VALUE);

        // Modifier les données du customer
        customer.setName("Jane Smith");
        customer.setEmail("jane.smith@example.com");
        customer.setMobileNumber("0698765432");

        // Recréer CustomerUserDetails avec les nouvelles données
        CustomerUserDetails janeUserDetails = new CustomerUserDetails(customer);
        when(authentication.getPrincipal()).thenReturn(janeUserDetails);

        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        String token = jwtUtil.generateJwtToken(authentication);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Vérifier avec le secret par défaut
        SecretKey defaultSecretKey = Keys.hmacShaKeyFor(
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE.getBytes(StandardCharsets.UTF_8)
        );

        Claims claims = Jwts.parser().verifyWith(defaultSecretKey)
                .build().parseSignedClaims(token).getPayload();

        assertEquals("Jane Smith", claims.get("username"));
        assertEquals("ROLE_USER", claims.get("roles"));
    }

    @Test
    @DisplayName("Devrait gérer gracieusement les authorities vides")
    void generateJwtToken_WithEmptyAuthorities_ShouldHandleGracefully() {
        // Arrange
        when(environment.getProperty(ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE))
                .thenReturn("mySuperSecretKeyThatIsLongEnoughForHS512");

        customer.setName("Test User");
        customer.setEmail("test@example.com");
        customer.setMobileNumber("0611223344");
        customer.setRoles(Set.of());  // Pas de rôles

        CustomerUserDetails testUserDetails = new CustomerUserDetails(customer);
        when(authentication.getPrincipal()).thenReturn(testUserDetails);

        // Aucune autorité
        Collection<GrantedAuthority> authorities = List.of();
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        String token = jwtUtil.generateJwtToken(authentication);

        // Assert
        assertNotNull(token);

        Claims claims = Jwts.parser().verifyWith(secretKey)
                .build().parseSignedClaims(token).getPayload();

        assertEquals("", claims.get("roles")); // Roles devrait être une string vide
    }

    @Test
    @DisplayName("Devrait définir correctement la date d'expiration (1 heure)")
    void generateJwtToken_ShouldSetCorrectExpiration() {
        // Arrange
        when(environment.getProperty(ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE))
                .thenReturn("mySuperSecretKeyThatIsLongEnoughForHS512");

        when(authentication.getPrincipal()).thenReturn(customerUserDetails);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        // Act
        String token = jwtUtil.generateJwtToken(authentication);

        // Assert
        Claims claims = Jwts.parser().verifyWith(secretKey)
                .build().parseSignedClaims(token).getPayload();

        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getIssuedAt());

        // Vérifier que l'expiration est dans 1 heure
        long expectedExpiration = claims.getIssuedAt().getTime() + 60 * 60 * 1000;
        assertEquals(expectedExpiration, claims.getExpiration().getTime());
    }

    @Test
    @DisplayName("Devrait refléter différentes données client dans le token")
    void generateJwtToken_WithDifferentCustomerData_ShouldReflectInToken() {
        // Arrange
        when(environment.getProperty(ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE))
                .thenReturn("mySuperSecretKeyThatIsLongEnoughForHS512");

        customer.setName("Different User");
        customer.setEmail("different@example.com");
        customer.setMobileNumber("0677889900");

        CustomerUserDetails differentUserDetails = new CustomerUserDetails(customer);
        when(authentication.getPrincipal()).thenReturn(differentUserDetails);

        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_MANAGER"),
                new SimpleGrantedAuthority("ROLE_VIEWER")
        );
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        String token = jwtUtil.generateJwtToken(authentication);

        // Assert
        Claims claims = Jwts.parser().verifyWith(secretKey)
                .build().parseSignedClaims(token).getPayload();

        assertEquals("Different User", claims.get("username"));
        assertEquals("different@example.com", claims.get("email"));
        assertEquals("0677889900", claims.get("mobileNumber"));
        assertEquals("ROLE_MANAGER,ROLE_VIEWER", claims.get("roles"));
    }

    @Test
    @DisplayName("Devrait valider correctement un token JWT valide")
    void validateJwtToken_WithValidToken_ShouldReturnTrue() {
        // Arrange
        when(environment.getProperty(ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE))
                .thenReturn("mySuperSecretKeyThatIsLongEnoughForHS512");

        when(authentication.getPrincipal()).thenReturn(customerUserDetails);
        when(authentication.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtUtil.generateJwtToken(authentication);

        // Act
        boolean isValid = jwtUtil.validateJwtToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Devrait rejeter un token JWT invalide")
    void validateJwtToken_WithInvalidToken_ShouldReturnFalse() {
        // Arrange
        when(environment.getProperty(ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE))
                .thenReturn("mySuperSecretKeyThatIsLongEnoughForHS512");

        String invalidToken = "invalid.jwt.token";

        // Act
        boolean isValid = jwtUtil.validateJwtToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Devrait extraire correctement l'email du token")
    void getEmailFromJwtToken_ShouldReturnCorrectEmail() {
        // Arrange
        when(environment.getProperty(ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE))
                .thenReturn("mySuperSecretKeyThatIsLongEnoughForHS512");

        when(authentication.getPrincipal()).thenReturn(customerUserDetails);
        when(authentication.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtUtil.generateJwtToken(authentication);

        // Act
        String email = jwtUtil.getEmailFromJwtToken(token);

        // Assert
        assertEquals("john.doe@example.com", email);
    }
}*/