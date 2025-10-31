/*package com.store.store.filter;

import com.store.store.constants.ApplicationConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Permet les stubs "inutiles"
class JWTTokenValidatorFilterTest {

    @Mock
    private Environment environment;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JWTTokenValidatorFilter jwtFilter;
    private SecretKey secretKey;
    private String validToken;

    @BeforeEach
    void setUp() {
        List<String> publicPaths = List.of("/api/public/**", "/auth/login");

        // Créer le spy
        JWTTokenValidatorFilter realFilter = new JWTTokenValidatorFilter(publicPaths);
        jwtFilter = spy(realFilter);

        String secret = "mySuperSecretKeyThatIsLongEnoughForHS512";
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        // Créer un token JWT valide pour les tests
        validToken = Jwts.builder()
                .subject("test@example.com")
                .claim("email", "test@example.com")
                .claim("roles", "ROLE_USER,ROLE_ADMIN")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000000))
                .signWith(secretKey)
                .compact();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_WithValidToken_ShouldSetAuthentication() throws Exception {
        // Arrange
        when(jwtFilter.getEnvironment()).thenReturn(environment);
        String authHeader = "Bearer " + validToken;
        when(request.getHeader(ApplicationConstants.JWT_HEADER)).thenReturn(authHeader);
        when(environment.getProperty(ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE)).thenReturn("mySuperSecretKeyThatIsLongEnoughForHS512");

        // Act
        jwtFilter.doFilter(request, response, filterChain);

        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("test@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithExpiredToken_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        when(jwtFilter.getEnvironment()).thenReturn(environment);
        when(environment.getProperty(ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE)).thenReturn("mySuperSecretKeyThatIsLongEnoughForHS512");

        String expiredToken = createExpiredToken();
        String authHeader = "Bearer " + expiredToken;
        when(request.getHeader(ApplicationConstants.JWT_HEADER)).thenReturn(authHeader);

        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        when(response.getWriter()).thenReturn(mockResponse.getWriter());

        // Act
        jwtFilter.doFilter(request, mockResponse, filterChain);

        // Assert
        assertEquals(401, mockResponse.getStatus());
        assertTrue(mockResponse.getContentAsString().contains("Token Expired"));
        verify(filterChain, never()).doFilter(request, mockResponse);
    }

    @Test
    void doFilterInternal_WithInvalidToken_ShouldThrowBadCredentials() throws Exception {
        // Arrange
        when(jwtFilter.getEnvironment()).thenReturn(environment);
        String authHeader = "Bearer invalid-token";
        when(request.getHeader(ApplicationConstants.JWT_HEADER)).thenReturn(authHeader);
        when(environment.getProperty(ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE)).thenReturn("mySuperSecretKeyThatIsLongEnoughForHS512");

        // Act & Assert
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> {
            jwtFilter.doFilter(request, response, filterChain);
        });

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithoutToken_ShouldContinueFilterChain() throws Exception {
        // Arrange
        when(request.getHeader(ApplicationConstants.JWT_HEADER)).thenReturn(null);

        // Act
        jwtFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotFilter_PublicPath_ShouldReturnTrue() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/public/data");

        // Act
        boolean result = jwtFilter.shouldNotFilter(request);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_PrivatePath_ShouldReturnFalse() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/private/data");

        // Act
        boolean result = jwtFilter.shouldNotFilter(request);

        // Assert
        assertFalse(result);
    }

    @Test
    void doFilterInternal_WithNullSecret_ShouldContinueWithoutAuthentication() throws Exception {
        // Arrange
        when(jwtFilter.getEnvironment()).thenReturn(environment);
        String authHeader = "Bearer " + validToken;
        when(request.getHeader(ApplicationConstants.JWT_HEADER)).thenReturn(authHeader);
        when(environment.getProperty(ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE)).thenReturn(null);

        // Act
        jwtFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithMalformedToken_ShouldThrowBadCredentials() throws Exception {
        // Arrange
        when(jwtFilter.getEnvironment()).thenReturn(environment);
        String authHeader = "Bearer malformed.token.here";
        when(request.getHeader(ApplicationConstants.JWT_HEADER)).thenReturn(authHeader);
        when(environment.getProperty(ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE)).thenReturn("mySuperSecretKeyThatIsLongEnoughForHS512");

        // Act & Assert
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> {
            jwtFilter.doFilter(request, response, filterChain);
        });

        verify(filterChain, never()).doFilter(request, response);
    }

    private String createExpiredToken() {
        return Jwts.builder()
                .subject("test@example.com")
                .claim("email", "test@example.com")
                .claim("roles", "ROLE_USER")
                .issuedAt(new Date(System.currentTimeMillis() - 100000))
                .expiration(new Date(System.currentTimeMillis() - 50000)) // Expiré
                .signWith(secretKey)
                .compact();
    }
}*/