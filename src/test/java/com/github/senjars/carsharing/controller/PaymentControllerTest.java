package com.github.senjars.carsharing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.senjars.carsharing.config.TestSecurityConfig;
import com.github.senjars.carsharing.dto.payment.CreatePaymentRequestDto;
import com.github.senjars.carsharing.dto.payment.PaymentResponseDto;
import com.github.senjars.carsharing.exception.EntityNotFoundException;
import com.github.senjars.carsharing.model.payment.PaymentStatus;
import com.github.senjars.carsharing.model.payment.PaymentType;
import com.github.senjars.carsharing.model.user.Role;
import com.github.senjars.carsharing.model.user.RoleName;
import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.security.CustomUserDetailsService;
import com.github.senjars.carsharing.security.JwtUtil;
import com.github.senjars.carsharing.service.PaymentService;
import com.github.senjars.carsharing.stripe.StripeProvider;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = PaymentController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(TestSecurityConfig.class)
public class PaymentControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private StripeProvider stripeProvider;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Should successfully initiate a payment session for a valid rental")
    void createPayment_validRequest_returnsPaymentResponseDto() throws Exception {
        // GIVEN
        User mockUser = createUser();
        CreatePaymentRequestDto requestDto = new CreatePaymentRequestDto(1L, PaymentType.PAYMENT);
        PaymentResponseDto responseDto = createPaymentResponseDto();

        when(paymentService.createPayment(anyLong(), eq(requestDto.rentalId()), eq(requestDto.type())))
                .thenReturn(responseDto);

        // WHEN & THEN
        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(user(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(responseDto.id()))
                .andExpect(jsonPath("$.rentalId").value(responseDto.rentalId()))
                .andExpect(jsonPath("$.status").value(responseDto.status().name()))
                .andExpect(jsonPath("$.type").value(responseDto.type().name()))
                .andExpect(jsonPath("$.amountToPay").value(responseDto.amountToPay().doubleValue()));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when payment request data is missing or invalid")
    @WithMockUser(username = "testUser", roles = "CUSTOMER")
    void createPayment_invalidRequest_returnsBadRequest() throws Exception {
        // GIVEN
        CreatePaymentRequestDto requestDto = new CreatePaymentRequestDto(null, null);

        // WHEN & THEN
        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 Not Found when attempting to pay for a non-existent rental")
    void createPayment_rentalNotFound_returnsNotFound() throws Exception {
        // GIVEN
        Long invalidId = 999L;
        User mockUser = createUser();
        CreatePaymentRequestDto requestDto = new CreatePaymentRequestDto(invalidId, PaymentType.PAYMENT);

        when(paymentService.createPayment(anyLong(), anyLong(), eq(PaymentType.PAYMENT)))
                .thenThrow(new EntityNotFoundException("Rental not found"));

        // WHEN & THEN
        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(user(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should successfully renew an existing payment session for a rental")
    void renewPayment_validRequest_returnsPaymentResponseDto() throws Exception {
        // GIVEN
        User mockUser = createUser();
        PaymentResponseDto responseDto = createPaymentResponseDto();

        when(paymentService.renewExistingPayment(anyLong(), eq(1L), eq(PaymentType.PAYMENT)))
                .thenReturn(responseDto);

        // WHEN & THEN
        mockMvc.perform(post("/api/payments/renew")
                        .with(csrf())
                        .with(user(mockUser))
                        .param("rentalId", "1")
                        .param("type", PaymentType.PAYMENT.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(responseDto.id()))
                .andExpect(jsonPath("$.rentalId").value(responseDto.rentalId()))
                .andExpect(jsonPath("$.status").value(responseDto.status().name()));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when renewal rentalId parameter is malformed")
    @WithMockUser(username = "testUser", roles = "CUSTOMER")
    void renewPayment_invalidRequest_returnsBadRequest() throws Exception {
        // WHEN & THEN
        mockMvc.perform(post("/api/payments/renew")
                        .with(csrf())
                        .param("rentalId", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return a paginated list of payments for the authenticated user")
    void getPayments_validRequest_returnsPageOfPaymentResponseDto() throws Exception {
        // GIVEN
        User mockUser = createUser();
        Pageable pageable = PageRequest.of(0, 10);
        PaymentResponseDto responseDto = createPaymentResponseDto();
        Page<PaymentResponseDto> paymentPage = new PageImpl<>(List.of(responseDto), pageable, 1);

        when(paymentService.getPaymentsByUserId(any(), any(), any(Pageable.class)))
                .thenReturn(paymentPage);

        // WHEN & THEN
        mockMvc.perform(get("/api/payments")
                        .with(user(mockUser))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(responseDto.id()));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when unauthenticated user tries to access payment history")
    void getPayments_unauthenticatedUser_returnsForbidden() throws Exception {
        // WHEN & THEN
        mockMvc.perform(get("/api/payments")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should successfully process a completed Stripe session and return payment details")
    void handleSuccessfulPayment_validRequest_returnsPaymentResponseDto() throws Exception {
        // GIVEN
        PaymentResponseDto responseDto = createPaymentResponseDto();

        when(paymentService.fulfillPayment("cs_test_session"))
                .thenReturn(responseDto);

        // WHEN & THEN
        mockMvc.perform(get("/api/payments/success")
                        .param("sessionId", "cs_test_session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(responseDto.id()));
    }

    @Test
    @DisplayName("Should return 200 OK with cancellation message when payment is cancelled by user")
    void handleCancel_validRequest_returnsOk() throws Exception {
        // GIVEN
        when(paymentService.handleCancel())
                .thenReturn("Payment cancelled");

        // WHEN & THEN
        mockMvc.perform(get("/api/payments/cancel"))
                .andExpect(status().isOk());
    }

    private PaymentResponseDto createPaymentResponseDto() {
        return new PaymentResponseDto(
                1L,
                101L,
                PaymentStatus.PENDING,
                PaymentType.PAYMENT,
                new BigDecimal("299.99"),
                "cs_test_a1b2c3d4e5f6",
                "https://checkout.stripe.com/pay/cctest_123"
        );
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setPassword("123Password123!");
        user.setFirstName("John");
        user.setLastName("Doe");
        RoleName roleName = RoleName.CUSTOMER;
        Role role = new Role();
        role.setName(roleName);
        user.setRoles(Set.of(role));
        return user;
    }
}
