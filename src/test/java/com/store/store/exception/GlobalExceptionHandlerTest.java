//package com.store.store.exception;

//import com.store.store.dto.ErrorResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
/*
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        MessageSource messageSource = mock(MessageSource.class);
        exceptionHandler = new GlobalExceptionHandler(messageSource);
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");

        // Configuration de base pour MessageSource
        when(messageSource.getMessage(anyString(), any(), anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("ResourceNotFoundException Handling")
    class ResourceNotFoundExceptionHandling {

        @Test
        @DisplayName("Should return 404 with proper error structure when resource not found")
        void handleResourceNotFound_ShouldReturn404WithProperStructure() {
            // Given
            ResourceNotFoundException exception =
                    new ResourceNotFoundException("Commande", "id", "123");

            // When
            ResponseEntity<ErrorResponseDto> response =
                    exceptionHandler.handleResourceNotFound(exception, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatusCode()).isEqualTo(404);
            assertThat(response.getBody().getStatus()).isEqualTo("Not Found");
            assertThat(response.getBody().getMessage()).contains("Commande");
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("BusinessException Handling")
    class BusinessExceptionHandling {

        @Test
        @DisplayName("Should return 400 with business error message")
        void handleBusinessException_ShouldReturn400WithBusinessMessage() {
            // Given
            String errorMessage = "Solde insuffisant pour effectuer la transaction";
            BusinessException exception = new BusinessException(errorMessage);

            // When
            ResponseEntity<ErrorResponseDto> response =
                    exceptionHandler.handleBusinessException(exception, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatusCode()).isEqualTo(400);
            assertThat(response.getBody().getStatus()).isEqualTo("Bad Request");
            assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        }
    }

    @Nested
    @DisplayName("ValidationException Handling")
    class ValidationExceptionHandling {

        @Test
        @DisplayName("Should return 400 with validation errors map")
        void handleValidationException_ShouldReturn400WithValidationErrors() {
            // Given
            Map<String, String> errors = Map.of(
                    "email", "doit être une adresse email valide",
                    "quantity", "doit être supérieur à 0"
            );
            ValidationException exception = new ValidationException(errors);

            // When
            ResponseEntity<ErrorResponseDto> response =
                    exceptionHandler.handleValidationException(exception, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatusCode()).isEqualTo(400);
            assertThat(response.getBody().getErrors()).hasSize(2);
            assertThat(response.getBody().getErrors()).containsAllEntriesOf(errors);
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        }
    }

    @Nested
    @DisplayName("AccessDeniedException Handling")
    class AccessDeniedExceptionHandling {

        @Test
        @DisplayName("Should return 403 for access denied scenarios")
        void handleAccessDenied_ShouldReturn403() {
            // Given
            AccessDeniedException exception = new AccessDeniedException("Accès refusé à la ressource");

            // When
            ResponseEntity<ErrorResponseDto> response =
                    exceptionHandler.handleAccessDenied(exception, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatusCode()).isEqualTo(403);
            assertThat(response.getBody().getStatus()).isEqualTo("Forbidden");
            assertThat(response.getBody().getMessage()).isEqualTo("Accès refusé");
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        }
    }

    @Nested
    @DisplayName("Generic Exception Handling")
    class GenericExceptionHandling {

        @Test
        @DisplayName("Should return 500 with trace ID for unexpected errors")
        void handleGlobalException_ShouldReturn500WithTraceId() {
            // Given
            RuntimeException exception = new RuntimeException("Erreur technique inattendue");

            // When
            ResponseEntity<ErrorResponseDto> response =
                    exceptionHandler.handleGlobalException(exception, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatusCode()).isEqualTo(500);
            assertThat(response.getBody().getStatus()).isEqualTo("Internal Server Error");
            assertThat(response.getBody().getMessage()).isEqualTo("Une erreur technique s'est produite");
            assertThat(response.getBody().getTraceId()).isNotBlank();
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        }
    }
}

 */