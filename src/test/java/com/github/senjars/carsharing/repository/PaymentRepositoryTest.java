package com.github.senjars.carsharing.repository;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.senjars.carsharing.model.car.Car;
import com.github.senjars.carsharing.model.car.CarType;
import com.github.senjars.carsharing.model.car.TypeName;
import com.github.senjars.carsharing.model.payment.Payment;
import com.github.senjars.carsharing.model.payment.PaymentStatus;
import com.github.senjars.carsharing.model.payment.PaymentType;
import com.github.senjars.carsharing.model.rental.Rental;
import com.github.senjars.carsharing.model.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CarTypeRepository carTypeRepository;

    @Test
    @DisplayName("Should return a paginated list of payments for a specific user ID")
    void findPaymentsByUserId_validUserId_returnsPayments() {
        // GIVEN
        Car car = carRepository.save(createCar());
        User user = userRepository.save(createUser());

        Rental rental = createRental(user.getId(), car.getId());
        rentalRepository.save(rental);

        paymentRepository.save(createPayment(rental.getId()));

        Pageable pageable = PageRequest.of(0, 10);

        // WHEN
        Page<Payment> paymentPage = paymentRepository.findPaymentsByUserId(user.getId(), pageable);

        // THEN
        assertThat(paymentPage.getContent()).isNotEmpty();
        assertThat(paymentPage.getTotalElements()).isEqualTo(1L);
        assertThat(paymentPage.getContent().get(0).getRentalId()).isEqualTo(rental.getId());
    }

    @Test
    @DisplayName("Should return an empty page when searching for payments of a non-existent user")
    void findPaymentsByUserId_invalidUserId_returnsEmptyPage() {
        // GIVEN
        Long invalidUserId = 999L;

        Pageable pageable = PageRequest.of(0, 10);

        // WHEN
        Page<Payment> paymentPage = paymentRepository.findPaymentsByUserId(invalidUserId, pageable);

        // THEN
        assertThat(paymentPage).isEmpty();
    }

    @Test
    @DisplayName("Should successfully find a payment record using Stripe session ID")
    void findPaymentBySessionId_validSessionId_returnsPayment() {
        // GIVEN
        User user = userRepository.save(createUser());
        Car car = carRepository.save(createCar());
        Rental rental = rentalRepository.save(createRental(user.getId(), car.getId()));
        Payment payment = paymentRepository.save(createPayment(rental.getId()));

        // WHEN
        Optional<Payment> foundPayment = paymentRepository
                .findPaymentBySessionId(payment.getSessionId());

        // THEN
        assertThat(foundPayment).isPresent();
        assertThat(foundPayment.get().getSessionId()).isEqualTo(payment.getSessionId());
    }

    @Test
    @DisplayName("Should return an empty optional when searching for a non-existent Stripe session ID")
    void findPaymentBySessionId_invalidSessionId_returnsEmpty() {
        // GIVEN
        String invalidSessionId = "invalid_session_id";

        // WHEN
        Optional<Payment> foundPayment = paymentRepository.findPaymentBySessionId(invalidSessionId);

        // THEN
        assertThat(foundPayment).isEmpty();
    }

    @Test
    @DisplayName("Should find all payments with a specific status created before a given date")
    void findAllByStatusAndCreatedAtBefore_validInput_returnsPayments() {
        // GIVEN
        User user = userRepository.save(createUser());
        Car car = carRepository.save(createCar());
        Rental rental = rentalRepository.save(createRental(user.getId(), car.getId()));
        Payment payment = paymentRepository.save(createPayment(rental.getId()));

        // WHEN
        List<Payment> payments = paymentRepository.findAllByStatusAndCreatedAtBefore(
                PaymentStatus.PAID, payment.getCreatedAt().plusSeconds(1));

        // THEN
        assertThat(payments).isNotEmpty();
        assertThat(payments.getFirst().getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("Should return an empty list when no payments match the status and date criteria")
    void findAllByStatusAndCreatedAtBefore_invalidInput_returnsEmpty() {
        // GIVEN
        LocalDate invalidDate = LocalDate.now().minusDays(1);

        // WHEN
        List<Payment> payments = paymentRepository.findAllByStatusAndCreatedAtBefore(
                PaymentStatus.PAID, invalidDate.atStartOfDay());

        // THEN
        assertThat(payments).isEmpty();
    }

    @Test
    @DisplayName("Should return true if at least one payment exists for a user with a specific status")
    void existsByUserIdAndStatus_validInput_returnsTrue() {
        // GIVEN
        Long userId = userRepository.save(createUser()).getId();
        Car car = carRepository.save(createCar());
        Rental rental = rentalRepository.save(createRental(userId,car.getId()));
        Payment payment = paymentRepository.save(createPayment(rental.getId()));

        // WHEN
        boolean exists = paymentRepository.existsByUserIdAndStatus(userId, PaymentStatus.PAID);

        // THEN
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false if no payments exist for the given user and status")
    void existsByUserIdAndStatus_invalidUserId_returnsFalse() {
        // GIVEN
        Long userId = 999L;

        // WHEN
        boolean exists = paymentRepository.existsByUserIdAndStatus(userId, PaymentStatus.PAID);

        // THEN
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should successfully retrieve payment information linked to a specific rental ID")
    void findByRentalId_validRentalId_returnsPayment() {
        // GIVEN
        Long userId = userRepository.save(createUser()).getId();
        Car car = carRepository.save(createCar());
        Rental rental = rentalRepository.save(createRental(userId, car.getId()));
        paymentRepository.save(createPayment(rental.getId()));

        // WHEN
        Optional<Payment> foundPayment = paymentRepository.findByRentalId(rental.getId());

        // THEN
        assertThat(foundPayment).isPresent();
        assertThat(foundPayment.get().getRentalId()).isEqualTo(rental.getId());
    }

    @Test
    @DisplayName("Should return an empty optional when searching for payment by a non-existent rental ID")
    void findByRentalId_InvalidRentalId_returnsEmpty() {
        // GIVEN
        Long invalidRentalId = 999L;

        // WHEN
        Optional<Payment> foundPayment = paymentRepository.findByRentalId(invalidRentalId);

        // THEN
        assertThat(foundPayment).isEmpty();
    }

    private User createUser() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("123Password123!");
        user.setFirstName("John");
        user.setLastName("Doe");
        return user;
    }

    private Rental createRental(Long userId, Long carId) {
        Rental rental = new Rental();
        rental.setUserId(userId);
        rental.setCarId(carId);
        rental.setRentalDate(LocalDate.now().minusDays(5));
        rental.setReturnDate(LocalDate.now().minusDays(2));
        return rental;
    }

    private Car createCar() {
        Car car = new Car();
        CarType type = new CarType();
        type.setTypeName(TypeName.SEDAN);
        car.setType(type);
        car.setModel("Test Model");
        car.setBrand("Test Brand");
        car.setInventory(1);
        car.setDailyFee(BigDecimal.valueOf(100));
        return car;
    }

    private Payment createPayment(Long rentalId) {
        Payment payment = new Payment();
        payment.setSessionId(randomUUID().toString());
        payment.setRentalId(rentalId);
        payment.setType(PaymentType.FINE);
        payment.setStatus(PaymentStatus.PAID);
        payment.setCreatedAt(LocalDate.now().atStartOfDay());
        payment.setSessionId(randomUUID().toString());
        payment.setSessionUrl("https://stripe.com/session/" + randomUUID().toString());
        return payment;
    }
}
