package com.github.senjars.carsharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.senjars.carsharing.dto.payment.PaymentResponseDto;
import com.github.senjars.carsharing.exception.AccessDeniedException;
import com.github.senjars.carsharing.exception.EntityNotFoundException;
import com.github.senjars.carsharing.exception.PaymentAlreadyProcessedException;
import com.github.senjars.carsharing.mapper.PaymentMapper;
import com.github.senjars.carsharing.model.car.Car;
import com.github.senjars.carsharing.model.car.CarType;
import com.github.senjars.carsharing.model.car.TypeName;
import com.github.senjars.carsharing.model.payment.Payment;
import com.github.senjars.carsharing.model.payment.PaymentStatus;
import com.github.senjars.carsharing.model.payment.PaymentType;
import com.github.senjars.carsharing.model.rental.Rental;
import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.notify.TelegramService;
import com.github.senjars.carsharing.repository.CarRepository;
import com.github.senjars.carsharing.repository.PaymentRepository;
import com.github.senjars.carsharing.repository.RentalRepository;
import com.github.senjars.carsharing.repository.UserRepository;
import com.github.senjars.carsharing.service.impl.PaymentServiceImpl;
import com.github.senjars.carsharing.stripe.StripeProvider;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private CarRepository carRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StripeProvider stripeProvider;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private TelegramService telegramService;

    @Test
    @DisplayName("Should successfully initiate a Stripe session and create a payment for a valid rental")
    void createPayment_validRental_returnsPaymentResponseDto() throws StripeException {
        // GIVEN
        Long userId = 1L;
        Long rentalId = 1L;
        Rental rental = createRental();
        Car car = createCar();
        Session session = createSession();
        Payment payment = createPayment();
        PaymentResponseDto expectedResponse = createPaymentResponseDto();

        // WHEN
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
        when(paymentRepository.findByRentalId(rentalId)).thenReturn(Optional.empty());
        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));
        when(stripeProvider.createSession(any(BigDecimal.class), any(String.class))).thenReturn(session);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(expectedResponse);

        PaymentResponseDto result = paymentService.createPayment(userId, rentalId);

        // THEN
        assertThat(result).isEqualTo(expectedResponse);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when attempting to create payment for a non-existent rental")
    void createPayment_invalidRentalId_throwsEntityNotFoundException() {
        // GIVEN
        Long userId = 1L;
        Long invalidRentalId = 999L;

        // WHEN
        when(rentalRepository.findById(invalidRentalId)).thenReturn(Optional.empty());

        // THEN
        assertThrows(EntityNotFoundException.class,
                () -> paymentService.createPayment(userId, invalidRentalId));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when a user tries to create a payment for someone else's rental")
    void createPayment_accessDenied_throwsAccessDeniedException() {
        // GIVEN
        Long userId = 1L;
        Long rentalId = 1L;
        Rental rental = createRental();
        rental.setUserId(2L);

        // WHEN
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

        // THEN
        assertThrows(AccessDeniedException.class,
                () -> paymentService.createPayment(userId, rentalId));
    }

    @Test
    @DisplayName("Should throw PaymentAlreadyProcessedException when trying to pay for an already completed rental")
    void createPayment_paymentAlreadyPaid_throwsPaymentAlreadyProcessedException() {
        // GIVEN
        Long userId = 1L;
        Long rentalId = 1L;
        Rental rental = createRental();
        Payment payment = createPayment();
        payment.setStatus(PaymentStatus.PAID);

        // WHEN
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
        when(paymentRepository.findByRentalId(rentalId)).thenReturn(Optional.of(payment));

        // THEN
        assertThrows(PaymentAlreadyProcessedException.class,
                () -> paymentService.createPayment(userId, rentalId));
    }

    @Test
    @DisplayName("Should return existing pending payment details instead of creating a new Stripe session")
    void createPayment_pendingPaymentExists_returnsPendingPayment() {
        // GIVEN
        Long userId = 1L;
        Long rentalId = 1L;
        Rental rental = createRental();
        Payment payment = createPayment();
        payment.setStatus(PaymentStatus.PENDING);
        PaymentResponseDto expectedResponse = createPaymentResponseDto();

        // WHEN
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
        when(paymentRepository.findByRentalId(rentalId)).thenReturn(Optional.of(payment));
        when(paymentMapper.toDto(payment)).thenReturn(expectedResponse);

        PaymentResponseDto result = paymentService.createPayment(userId, rentalId);

        // THEN
        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("Should return a paginated list of payments when a user accesses their own payment history")
    void getPaymentsByUserId_validUserId_returnsPayments() {
        // GIVEN
        Long userId = 1L;
        User user = createUser();
        Pageable pageable = PageRequest.of(0, 10);
        Payment payment = createPayment();
        Page<Payment> paymentPage = new PageImpl<>(java.util.List.of(payment), pageable, 1);
        PaymentResponseDto expectedDto = createPaymentResponseDto();
        Page<PaymentResponseDto> expectedPage = new PageImpl<>(
                java.util.List.of(expectedDto), pageable, 1);

        // WHEN
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(paymentRepository.findPaymentsByUserId(userId, pageable)).thenReturn(paymentPage);
        when(paymentMapper.toDto(payment)).thenReturn(expectedDto);

        Page<PaymentResponseDto> result = paymentService.getPaymentsByUserId(user, userId, pageable);

        // THEN
        assertThat(result.getContent()).containsExactly(expectedDto);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when trying to view payment history of another user")
    void getPaymentsByUserId_accessDenied_throwsAccessDeniedException() {
        // GIVEN
        Long userId = 1L;
        User currentUser = createUser();
        currentUser.setId(2L);
        Pageable pageable = PageRequest.of(0, 10);

        // WHEN
        when(userRepository.findById(userId)).thenReturn(Optional.of(createUser()));

        // THEN
        assertThrows(AccessDeniedException.class,
                () -> paymentService.getPaymentsByUserId(currentUser, userId, pageable));
    }

    @Test
    @DisplayName("Should update payment status to PAID and finalize the process after successful Stripe checkout")
    void fulfillPayment_validSession_marksPaidAndReturnsDto() {
        // GIVEN
        String sessionId = "test_session_id";
        Payment payment = createPayment();
        payment.setStatus(PaymentStatus.PENDING);
        Session session = createSession();
        PaymentResponseDto expectedDto = createPaymentResponseDto();

        // WHEN
        when(paymentRepository.findPaymentBySessionId(sessionId)).thenReturn(Optional.of(payment));
        when(stripeProvider.getSession(sessionId)).thenReturn(session);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(expectedDto);

        PaymentResponseDto result = paymentService.fulfillPayment(sessionId);

        // THEN
        assertThat(result).isEqualTo(expectedDto);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when finalizing a payment with an unrecognized session ID")
    void fulfillPayment_invalidSessionId_throwsEntityNotFoundException() {
        // GIVEN
        String invalidSessionId = "invalid_id";

        // WHEN
        when(paymentRepository.findPaymentBySessionId(invalidSessionId)).thenReturn(Optional.empty());

        // THEN
        assertThrows(EntityNotFoundException.class,
                () -> paymentService.fulfillPayment(invalidSessionId));
    }

    @Test
    @DisplayName("Should return a user-friendly cancellation message when a payment process is aborted")
    void handleCancel_returnsSuccessMessage() {
        // WHEN
        String result = paymentService.handleCancel();

        // THEN
        assertThat(result).contains("Payment cancelled successfully");
    }

    private Car createCar() {
        Car car = new Car();
        car.setId(1L);
        CarType type = new CarType();
        type.setTypeName(TypeName.SEDAN);
        car.setType(type);
        car.setModel("Test Model");
        car.setBrand("Test Brand");
        car.setInventory(1);
        car.setDailyFee(BigDecimal.valueOf(100));
        return car;
    }

    private Rental createRental() {
        Rental rental = new Rental();
        rental.setId(1L);
        rental.setUserId(1L);
        rental.setCarId(1L);
        rental.setRentalDate(LocalDate.now());
        rental.setReturnDate(LocalDate.now().plusDays(3));
        return rental;
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        return user;
    }

    private Payment createPayment() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setType(PaymentType.PAYMENT);
        payment.setAmountToPay(BigDecimal.valueOf(300));
        payment.setRentalId(1L);
        payment.setSessionId("test_session_id");
        payment.setCreatedAt(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }

    private PaymentResponseDto createPaymentResponseDto() {
        return new PaymentResponseDto(
                "1",
                "1",
                PaymentStatus.PENDING,
                PaymentType.PAYMENT,
                BigDecimal.valueOf(300),
                "test_session_id",
                "https://test.stripe.com/pay/test_session_id"
        );
    }

    private Session createSession() {
        Session session = new Session();
        session.setId("test_session_id");
        session.setUrl("https://test.stripe.com/pay/test_session_id");
        session.setPaymentStatus("paid");
        return session;
    }
}