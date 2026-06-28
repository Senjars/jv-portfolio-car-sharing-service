package com.github.senjars.carsharing.service.impl;

import com.github.senjars.carsharing.dto.payment.PaymentResponseDto;
import com.github.senjars.carsharing.exception.AccessDeniedException;
import com.github.senjars.carsharing.exception.EntityNotFoundException;
import com.github.senjars.carsharing.exception.PaymentAlreadyProcessedException;
import com.github.senjars.carsharing.exception.PaymentException;
import com.github.senjars.carsharing.mapper.PaymentMapper;
import com.github.senjars.carsharing.model.car.Car;
import com.github.senjars.carsharing.model.payment.Payment;
import com.github.senjars.carsharing.model.payment.PaymentStatus;
import com.github.senjars.carsharing.model.payment.PaymentType;
import com.github.senjars.carsharing.model.rental.Rental;
import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.notify.NotificationService;
import com.github.senjars.carsharing.repository.CarRepository;
import com.github.senjars.carsharing.repository.PaymentRepository;
import com.github.senjars.carsharing.repository.RentalRepository;
import com.github.senjars.carsharing.repository.UserRepository;
import com.github.senjars.carsharing.service.PaymentService;
import com.github.senjars.carsharing.stripe.StripeProvider;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final double FINE_MULTIPLIER = 1.5;

    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final StripeProvider stripeProvider;
    private final PaymentMapper paymentMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public PaymentResponseDto createPayment(Long userId, Long rentalId, PaymentType type) {
        Rental rental = getVerifiedRental(userId, rentalId);

        Optional<Payment> existingPayment = paymentRepository.findByRentalIdAndType(rentalId, type);

        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();

            if (payment.getStatus() == PaymentStatus.PAID) {
                throw new PaymentAlreadyProcessedException("Payment for rental with id: "
                        + rentalId + " already exists and is paid");
            }

            if (payment.getStatus() == PaymentStatus.EXPIRED) {
                return renewExistingPayment(userId, rentalId, type);
            }

            if (payment.getStatus() == PaymentStatus.PENDING) {
                return paymentMapper.toDto(payment);
            }
        }

        return createNewPayment(rental, type);
    }

    @Override
    @Transactional
    public PaymentResponseDto renewExistingPayment(Long userId, Long rentalId, PaymentType type) {
        getVerifiedRental(userId, rentalId);

        Payment payment = paymentRepository.findByRentalIdAndType(rentalId, type)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));

        return internalRenew(payment, rentalId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getPaymentsByUserId(User user,
                                                        Long userId, Pageable pageable) {
        boolean manager = isManager(user);

        if (manager && userId == null) {
            return paymentRepository.findAll(pageable).map(paymentMapper::toDto);
        }

        if (!manager && userId != null && !Objects.equals(user.getId(), userId)) {
            throw new AccessDeniedException("You cannot view payments of another user");
        }

        Long targetUserId = (manager && userId != null) ? userId : user.getId();

        if (userRepository.findById(targetUserId).isEmpty()) {
            throw new EntityNotFoundException("User not found");
        }

        return paymentRepository.findPaymentsByUserId(targetUserId, pageable)
                .map(paymentMapper::toDto);
    }

    @Override
    @Transactional
    public PaymentResponseDto fulfillPayment(String sessionId) {
        Payment payment = paymentRepository.findPaymentBySessionId(sessionId).orElseThrow(
                () -> new EntityNotFoundException("Payment with session ID: "
                        + sessionId + " not found"));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return paymentMapper.toDto(payment);
        }

        Session session = stripeProvider.getSession(sessionId);

        if (session == null) {
            throw new EntityNotFoundException("Payment with session ID: "
                    + sessionId + " not found");
        }

        if (!"paid".equals(session.getPaymentStatus())) {
            throw new PaymentAlreadyProcessedException("Payment with session ID: "
                    + sessionId + " is not paid yet");
        }

        payment.setStatus(PaymentStatus.PAID);
        Payment savedPayment = paymentRepository.save(payment);

        try {
            notificationService.sendMessage(String.format(
                    "✅ **Payment Confirmed**\n\n"
                            + "💰 **Amount:** %s\n"
                            + "🆔 **Payment ID:** %d\n"
                            + "🧾 **Rental ID:** %d\n"
                            + "📌 **Type:** %s",
                    savedPayment.getAmountToPay(),
                    savedPayment.getId(),
                    savedPayment.getRental().getId(),
                    savedPayment.getType()
            ));
        } catch (Exception e) {
            log.warn("Failed to send Telegram notification", e);
        }

        return paymentMapper.toDto(savedPayment);
    }

    @Override
    @Transactional
    public String handleCancel() {
        return "Payment cancelled successfully. No charges were made to your account.";
    }

    @Override
    @Transactional
    public void processWebhook(String payload, String sigHeader) {
        try {
            Event event = stripeProvider.getWebhookEvent(payload, sigHeader);

            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() -> new PaymentException("Empty session in webhook event"));

                this.fulfillPayment(session.getId());
            }
        } catch (StripeException e) {
            throw new PaymentException("Webhook processing failed" + ": " + e.getMessage());
        }
    }

    private BigDecimal calculatePayment(Rental rental, PaymentType type) {
        long plannedDays = ChronoUnit.DAYS.between(rental.getRentalDate(), rental.getReturnDate());

        Car car = carRepository.findById(rental.getCarId()).orElseThrow(
                () -> new EntityNotFoundException("Car not found"));

        BigDecimal paymentAmount = car.getDailyFee()
                .multiply(BigDecimal.valueOf(Math.max(plannedDays, 1)));

        if (type == PaymentType.FINE) {
            LocalDate actualDate = rental.getActualReturnDate() != null
                    ? rental.getActualReturnDate()
                    : LocalDate.now();

            long daysLate = ChronoUnit.DAYS.between(rental.getReturnDate(), actualDate);

            BigDecimal fineValue = car.getDailyFee()
                    .multiply(BigDecimal.valueOf(Math.max(daysLate, 1)))
                    .multiply(BigDecimal.valueOf(FINE_MULTIPLIER));

            return paymentAmount.add(fineValue);
        }

        return paymentAmount;
    }

    private Rental getVerifiedRental(Long userId, Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId).orElseThrow(
                () -> new EntityNotFoundException("Rental with id: " + rentalId + " not found"));

        if (!rental.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to create this payment");
        }
        return rental;
    }

    private PaymentResponseDto createNewPayment(Rental rental, PaymentType type) {
        BigDecimal amountToPay = calculatePayment(rental, type);
        String description = "Payment for rental with id: " + rental.getId();
        Session session = createStripeSession(amountToPay, description, rental.getId());

        Payment payment = new Payment();
        payment.setRental(rental);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmountToPay(amountToPay);
        payment.setSessionId(session.getId());
        payment.setSessionUrl(session.getUrl());
        payment.setType(type);

        return paymentMapper.toDto(paymentRepository.save(payment));
    }

    private Session createStripeSession(BigDecimal amountToPay, String description, Long rentalId) {
        Session session = null;
        try {
            session = stripeProvider.createSession(amountToPay, description);
        } catch (StripeException e) {
            throw new PaymentException("Error creating payment session: " + e.getMessage()
                    + ", rental id: " + rentalId);
        }
        return session;
    }

    private PaymentResponseDto internalRenew(Payment payment, Long rentalId) {
        String description = "Renewal for rental: " + rentalId;
        Session session = createStripeSession(payment.getAmountToPay(), description, rentalId);

        payment.setSessionId(session.getId());
        payment.setSessionUrl(session.getUrl());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        return paymentMapper.toDto(paymentRepository.save(payment));
    }

    private boolean isManager(User user) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
    }
}
