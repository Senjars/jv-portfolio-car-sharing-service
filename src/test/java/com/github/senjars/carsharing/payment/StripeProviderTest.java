package com.github.senjars.carsharing.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.lenient;

import com.github.senjars.carsharing.config.StripeConfig;
import com.github.senjars.carsharing.exception.PaymentException;
import com.github.senjars.carsharing.stripe.StripeProvider;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StripeProviderTest {

    @Mock
    private StripeConfig stripeConfig;

    @InjectMocks
    private StripeProvider stripeProvider;

    @BeforeEach
    void setUp() {
        lenient().when(stripeConfig.getAppUrl()).thenReturn("http://localhost:8080");
        lenient().when(stripeConfig.getSecretKey()).thenReturn("sk_test_123");
    }

    @Test
    @DisplayName("Should successfully retrieve a session by ID")
    void getSession_ValidId_ReturnsSession() throws StripeException {
        String sessionId = "cs_test_abc";
        Session mockSession = mock(Session.class);

        // Używamy try-with-resources dla statycznego mocka
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.retrieve(sessionId)).thenReturn(mockSession);

            Session result = stripeProvider.getSession(sessionId);

            assertNotNull(result);
            assertEquals(mockSession, result);
        }
    }

    @Test
    @DisplayName("Should throw PaymentException when Stripe retrieval fails")
    void getSession_StripeException_ThrowsPaymentException() {
        String sessionId = "invalid_id";

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.retrieve(sessionId))
                    .thenThrow(new StripeException("API Error", "id", "code", 500) {});

            assertThrows(PaymentException.class, () -> stripeProvider.getSession(sessionId));
        }
    }

    @Test
    @DisplayName("Should create a checkout session successfully")
    void createSession_ValidAmount_ReturnsSession() throws StripeException {
        BigDecimal amount = new BigDecimal("100.00");
        String description = "Rental for Tesla";
        Session mockSession = mock(Session.class);

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            Session result = stripeProvider.createSession(amount, description);

            assertNotNull(result);
            assertEquals(mockSession, result);
        }
    }

    @Test
    @DisplayName("Should correctly calculate amount in cents")
    void createSession_CalculatesCentsCorrectly() throws StripeException {
        // GIVEN
        BigDecimal amount = new BigDecimal("19.99");
        String description = "Test rental";
        Session mockSession = mock(Session.class);

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            // Konfigurujemy mocka, żeby zwracał sesję dla jakichkolwiek parametrów
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            // WHEN
            stripeProvider.createSession(amount, description);

            // THEN
            // Przechwytujemy argument przekazany do statycznej metody Session.create
            ArgumentCaptor<SessionCreateParams> captor = ArgumentCaptor.forClass(SessionCreateParams.class);
            mockedSession.verify(() -> Session.create(captor.capture()));

            SessionCreateParams capturedParams = captor.getValue();

            // Wyciągamy kwotę w centach z głębi struktury parametrów
            Long unitAmount = capturedParams.getLineItems().get(0).getPriceData().getUnitAmount();

            // Sprawdzamy czy 19.99 zamieniło się na 1999 centów
            assertEquals(1999L, unitAmount, "Amount in cents should be 1999 for $19.99");
        }
    }
}
