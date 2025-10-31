import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

// ============================================================
// 1. TESTS UNITAIRES - StoreSecurityConfigUnitTest.java
// ============================================================
/*package com.store.store.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests unitaires RAPIDES sans contexte Spring (< 1s).
 * Testent uniquement la logique de configuration pure.
 */
/*
class StoreSecurityConfigUnitTest {

    private StoreSecurityConfig config;
    private static final List<String> TEST_PUBLIC_PATHS = List.of("/api/public/**");
    private static final String TEST_ORIGINS = "http://localhost:5173,http://localhost:3000";

    @BeforeEach
    void setUp() {
        config = new StoreSecurityConfig(TEST_PUBLIC_PATHS);
        ReflectionTestUtils.setField(config, "allowedOrigins", TEST_ORIGINS);
    }

    @Test
    @DisplayName("CORS devrait parser correctement les origines multiples")
    void corsConfiguration_ShouldSplitOriginsCorrectly() {
        var corsConfig = config.corsConfigurationSource();
        var urlBasedSource = (org.springframework.web.cors.UrlBasedCorsConfigurationSource) corsConfig;
        var corsConf = urlBasedSource.getCorsConfigurations().get("/**");

        assertEquals(2, corsConf.getAllowedOrigins().size());
        assertTrue(corsConf.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(corsConf.getAllowedOrigins().contains("http://localhost:3000"));
        assertEquals(Boolean.TRUE, corsConf.getAllowCredentials());
        assertEquals(3600L, corsConf.getMaxAge());
    }

    @Test
    @DisplayName("getPublicPaths devrait retourner une copie immuable")
    void getPublicPaths_ShouldReturnImmutableCopy() {
        List<String> paths = config.getPublicPaths();

        assertNotNull(paths);
        assertFalse(paths.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> paths.add("/test"));
        assertThrows(UnsupportedOperationException.class, () -> paths.set(0, "/modified"));
    }

    @Test
    @DisplayName("PasswordEncoder devrait être BCrypt")
    void passwordEncoder_ShouldBeBCrypt() {
        var encoder = config.passwordEncoder();

        assertInstanceOf(
                org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.class,
                encoder
        );

        // Test fonctionnel minimal
        String encoded = encoder.encode("test");
        assertTrue(encoder.matches("test", encoded));
        assertFalse(encoder.matches("wrong", encoded));
    }

    @Test
    @DisplayName("CompromisedPasswordChecker devrait être HaveIBeenPwned")
    void compromisedPasswordChecker_ShouldBeHaveIBeenPwned() {
        var checker = config.compromisedPasswordChecker();

        assertInstanceOf(
                org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker.class,
                checker
        );
    }

    // À revoir apres la modification
    /*@Test
    @DisplayName("AuthenticationManager devrait rejeter les providers null")
    void authenticationManager_WithNullProvider_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> config.authenticationManager(null));
    }

    @Test
    @DisplayName("AuthenticationManager devrait encapsuler correctement le provider")
    void authenticationManager_ShouldWrapProviderCorrectly() {
        AuthenticationProvider mockProvider = mock(AuthenticationProvider.class);
        var manager = config.authenticationManager(mockProvider);

        assertInstanceOf(ProviderManager.class, manager);
        assertEquals(1, ((ProviderManager) manager).getProviders().size());
        assertSame(mockProvider, ((ProviderManager) manager).getProviders().get(0));
    }


}
*/