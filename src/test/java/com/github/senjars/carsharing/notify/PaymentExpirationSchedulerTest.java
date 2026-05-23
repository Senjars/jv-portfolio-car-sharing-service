package com.github.senjars.carsharing.notify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.senjars.carsharing.model.payment.Payment;
import com.github.senjars.carsharing.model.payment.PaymentStatus;
import com.github.senjars.carsharing.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentExpirationSchedulerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentExpirationScheduler paymentExpirationScheduler;

    @Test
    @DisplayName("Should do nothing when no expired payments are found")
    void checkExpiredPayments_NoExpired_NoUpdates() {
        // GIVEN
        when(paymentRepository.findAllByStatusAndCreatedAtBefore(
                eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // WHEN
        paymentExpirationScheduler.checkExpiredPayments();

        // THEN
        verify(paymentRepository, times(1))
                .findAllByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should update status to EXPIRED for all found pending expired payments")
    void checkExpiredPayments_FoundExpired_UpdatesStatus() {
        // GIVEN
        Payment payment1 = new Payment();
        payment1.setStatus(PaymentStatus.PENDING);

        Payment payment2 = new Payment();
        payment2.setStatus(PaymentStatus.PENDING);

        List<Payment> expiredPayments = List.of(payment1, payment2);

        when(paymentRepository.findAllByStatusAndCreatedAtBefore(
                eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(expiredPayments);

        // WHEN
        paymentExpirationScheduler.checkExpiredPayments();

        // THEN
        assertEquals(PaymentStatus.EXPIRED, payment1.getStatus());
        assertEquals(PaymentStatus.EXPIRED, payment2.getStatus());

        verify(paymentRepository, times(2)).save(any(Payment.class));
    }
}