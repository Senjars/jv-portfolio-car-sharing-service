package com.github.senjars.carsharing.controller;

import com.github.senjars.carsharing.dto.payment.CreatePaymentRequestDto;
import com.github.senjars.carsharing.dto.payment.PaymentResponseDto;
import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.service.PaymentService;
import com.github.senjars.carsharing.stripe.StripeProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final StripeProvider stripeProvider;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @Operation(
            summary = "Create payment session",
            description = "Creates a new payment session for a rental",
            responses = {
                    @ApiResponse(responseCode = "201",
                            description = "Payment session created successfully"),
                    @ApiResponse(responseCode = "400",
                            description = "Bad request"),
                    @ApiResponse(responseCode = "404",
                            description = "Rental not found")
            }
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER')")
    public PaymentResponseDto createPayment(@AuthenticationPrincipal User user,
                                          @Valid @RequestBody CreatePaymentRequestDto requestDto) {
        return paymentService.createPayment(user.getId(), requestDto.rentalId());
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/success")
    @Operation(
            summary = "Handle successful payment",
            description = "Handles successful payment completion from Stripe",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Payment processed successfully"),
                    @ApiResponse(responseCode = "404",
                            description = "Payment session not found")
            }
    )
    public PaymentResponseDto handleSuccessfulPayment(@RequestParam String sessionId) {
        return paymentService.fulfillPayment(sessionId);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/cancel")
    @Operation(
            summary = "Handle cancelled payment",
            description = "Handles cancelled payment session from Stripe",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Payment cancellation handled successfully"),
                    @ApiResponse(responseCode = "404",
                            description = "Payment session not found")
            }
    )
    public String handleCancel() {
        return paymentService.handleCancel();
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    @Operation(
            summary = "Get payments",
            description = "Retrieves payments by user ID",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Payments retrieved successfully"),
                    @ApiResponse(responseCode = "403",
                            description = "Access denied"),
                    @ApiResponse(responseCode = "404",
                            description = "User not found")
            }
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER')")
    public Page<PaymentResponseDto> getPayments(@AuthenticationPrincipal User user,
                                              @RequestParam(required = false) Long userId,
                                              Pageable pageable) {
        return paymentService.getPaymentsByUserId(user, userId, pageable);
    }

    @PostMapping("/webhook")
    @Operation(summary = "Stripe webhook", description = "Endpoint for Stripe events")
    public void handleWebhook(@RequestBody String payload,
                              @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.processWebhook(payload, sigHeader);
    }
}
