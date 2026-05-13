package com.github.senjars.carsharing.stripe;

import com.github.senjars.carsharing.config.StripeConfig;
import com.github.senjars.carsharing.exception.PaymentException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class StripeProvider {

    private final StripeConfig stripeConfig;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeConfig.getSecretKey();
    }

    public Session getSession(String sessionId) {
        try {
            return Session.retrieve(sessionId);
        } catch (StripeException e) {
            throw new PaymentException("Error retrieving payment session: " + e.getMessage());
        }
    }

    public Session createSession(BigDecimal amount, String description)
            throws StripeException {
        long amountInCents = amount.movePointRight(2).longValue();

        String baseUrl = stripeConfig.getAppUrl() + "/api/payments";

        String successUrl = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/success")
                .queryParam("sessionId", "{CHECKOUT_SESSION_ID}")
                .build().toUriString();

        String cancelUrl = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/cancel")
                .build().toUriString();

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.PAYPAL)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(amountInCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData
                                        .ProductData.builder()
                                        .setName("Car Rental Payment")
                                        .setDescription(description)
                                        .build())
                                .build())
                        .build())
                .build();

        return Session.create(params);
    }

    public Event getWebhookEvent(String payload, String sigHeader)
            throws SignatureVerificationException {
        return Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
    }
}
