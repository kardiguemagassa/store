package com.store.store.service.impl;

import com.store.store.dto.payment.PaymentIntentRequestDto;
import com.store.store.dto.payment.PaymentIntentResponseDto;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentIntentRequestDto validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new PaymentIntentRequestDto(1000L, "eur");
    }

    @Test
    @DisplayName("DEV-017: Créer un PaymentIntent - Doit réussir avec des paramètres valides")
    void createPaymentIntent_WithValidRequest_ShouldReturnClientSecret() throws Exception {
        // Given
        PaymentIntent mockPaymentIntent = createMockPaymentIntent("pi_test_secret_123456");

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(mockPaymentIntent);

            // When
            PaymentIntentResponseDto result = paymentService.createPaymentIntent(validRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.clientSecret()).isEqualTo("pi_test_secret_123456");

            paymentIntentMock.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)));
        }
    }

    @Test
    @DisplayName("DEV-018: Créer un PaymentIntent - Doit lancer une exception en cas d'erreur Stripe")
    void createPaymentIntent_WhenStripeFails_ShouldThrowRuntimeException() {
        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            // Given - Pas besoin de mock pour le message de l'exception
            StripeException stripeException = mock(StripeException.class);

            paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenThrow(stripeException);

            // When & Then
            assertThatThrownBy(() -> paymentService.createPaymentIntent(validRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Échec de paiement")
                    .hasCause(stripeException);
        }
    }

    @Test
    @DisplayName("DEV-019: Créer un PaymentIntent avec montant zéro - Doit être accepté")
    void createPaymentIntent_WithZeroAmount_ShouldProcessSuccessfully() throws Exception {
        // Given
        PaymentIntentRequestDto zeroAmountRequest = new PaymentIntentRequestDto(0L, "eur");
        PaymentIntent mockPaymentIntent = createMockPaymentIntent("pi_test_secret_789");

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(mockPaymentIntent);

            // When
            PaymentIntentResponseDto result = paymentService.createPaymentIntent(zeroAmountRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.clientSecret()).isEqualTo("pi_test_secret_789");
        }
    }

    @Test
    @DisplayName("DEV-020: Créer un PaymentIntent avec devise USD - Doit être accepté")
    void createPaymentIntent_WithUsdCurrency_ShouldProcessSuccessfully() throws Exception {
        // Given
        PaymentIntentRequestDto usdRequest = new PaymentIntentRequestDto(2000L, "usd");
        PaymentIntent mockPaymentIntent = createMockPaymentIntent("pi_test_secret_usd_456");

        try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
            paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(mockPaymentIntent);

            // When
            PaymentIntentResponseDto result = paymentService.createPaymentIntent(usdRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.clientSecret()).isEqualTo("pi_test_secret_usd_456");
        }
    }

    // Méthode utilitaire pour créer un mock de PaymentIntent avec clientSecret
    private PaymentIntent createMockPaymentIntent(String clientSecret) throws Exception {
        PaymentIntent paymentIntent = new PaymentIntent();

        // Utilisation de la réflexion pour définir le clientSecret
        Field clientSecretField = PaymentIntent.class.getDeclaredField("clientSecret");
        clientSecretField.setAccessible(true);
        clientSecretField.set(paymentIntent, clientSecret);

        return paymentIntent;
    }
}