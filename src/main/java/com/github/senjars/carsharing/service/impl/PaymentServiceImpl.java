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
import com.github.senjars.carsharing.notify.TelegramService;
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
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final StripeProvider stripeProvider;
    private final PaymentMapper paymentMapper;
    private final TelegramService telegramService;

    @Override
    @Transactional
    public PaymentResponseDto createPayment(Long userId, Long rentalId) {

        Rental rental = rentalRepository.findById(rentalId).orElseThrow(
                () -> new EntityNotFoundException("Rental with id: " + rentalId + " not found"));

        if (!rental.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to create this payment");
        }

        if (paymentRepository.existsByRentalId(rentalId)) {
            throw new PaymentAlreadyProcessedException("Payment for rental with id: "
                    + rentalId + " already exists");
        }

        BigDecimal amountToPay = calculatePayment(rental);
        String description = "Payment for rental with id: " + rentalId;

        Session session = null;
        try {
            session = stripeProvider.createSession(amountToPay, description);
        } catch (StripeException e) {
            throw new PaymentException("Error creating payment session: " + e.getMessage()
                    + ", rental id: " + rentalId);
        }

        PaymentType type = (rental.getActualReturnDate() != null
                && rental.getActualReturnDate().isAfter(rental.getReturnDate())
                ? PaymentType.FINE : PaymentType.PAYMENT);

        Payment payment = new Payment();
        payment.setRentalId(rentalId);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmountToPay(amountToPay);
        payment.setSessionId(session.getId());
        payment.setSessionUrl(session.getUrl());
        payment.setType(type);

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toDto(savedPayment);
    }

    @Override
    @Transactional
    public BigDecimal calculatePayment(Rental rental) {
        long plannedDays = ChronoUnit.DAYS.between(rental.getRentalDate(), rental.getReturnDate());

        Car car = carRepository.findById(rental.getCarId()).orElseThrow(
                () -> new EntityNotFoundException("Car not found"));

        BigDecimal paymentAmount = car.getDailyFee()
                .multiply(BigDecimal.valueOf(Math.max(plannedDays, 1)));

        if (rental.getActualReturnDate() != null
                && rental.getActualReturnDate().isAfter(rental.getReturnDate())) {

            double fineMultiplayer = 1.5;
            long daysLate = ChronoUnit.DAYS.between(rental.getReturnDate(),
                    rental.getActualReturnDate());

            BigDecimal fineValue = car.getDailyFee()
                    .multiply(BigDecimal.valueOf(daysLate))
                    .multiply(BigDecimal.valueOf(fineMultiplayer));

            paymentAmount = paymentAmount.add(fineValue);
            return paymentAmount;
        }

        return paymentAmount;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getPaymentsByUserId(User user,
                                                        Long userId, Pageable pageable) {

        if (userRepository.findById(userId).isEmpty()) {
            throw new EntityNotFoundException("User not found");
        }

        if (!Objects.equals(user.getId(), userId) && user.getAuthorities().stream().noneMatch(
                a -> a.getAuthority().equals("ROLE_MANAGER"))) {
            throw new AccessDeniedException("You cannot view this payment by another user");
        }

        return paymentRepository.findPaymentsByUserId(userId, pageable)
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
            telegramService.sendMessage(String.format(
                    "✅ **Payment Confirmed**\n\n"
                            + "💰 **Amount:** %s\n"
                            + "🆔 **Payment ID:** %d\n"
                            + "🧾 **Rental ID:** %d\n"
                            + "📌 **Type:** %s",
                    savedPayment.getAmountToPay(),
                    savedPayment.getId(),
                    savedPayment.getRentalId(),
                    savedPayment.getType()
            ));
        } catch (Exception e) {
            System.err.println("Failed to send Telegram notification: " + e.getMessage());
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
}
