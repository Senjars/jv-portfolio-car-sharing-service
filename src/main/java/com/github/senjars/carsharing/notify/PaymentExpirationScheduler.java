package com.github.senjars.carsharing.notify;

import com.github.senjars.carsharing.model.payment.Payment;
import com.github.senjars.carsharing.model.payment.PaymentStatus;
import com.github.senjars.carsharing.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentExpirationScheduler {

    private static final int EXPIRATION_HOURS = 24;
    private final PaymentRepository paymentRepository;

    @Transactional
    @Scheduled(fixedRateString = "PT24H")
    public void checkExpiredPayments() {
        LocalDateTime expirationThreshold = LocalDateTime.now().minusHours(EXPIRATION_HOURS);

        List<Payment> expiredPayments = paymentRepository.findAllByStatusAndCreatedAtBefore(
                PaymentStatus.PENDING, expirationThreshold);

        if (!expiredPayments.isEmpty()) {
            expiredPayments.forEach(payment -> {
                payment.setStatus(PaymentStatus.EXPIRED);
            });
            paymentRepository.saveAll(expiredPayments);
        }
    }
}
