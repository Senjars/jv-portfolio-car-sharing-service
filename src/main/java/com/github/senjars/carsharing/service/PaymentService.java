package com.github.senjars.carsharing.service;

import com.github.senjars.carsharing.dto.payment.PaymentResponseDto;
import com.github.senjars.carsharing.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    PaymentResponseDto createPayment(Long userId, Long rentalId);

    Page<PaymentResponseDto> getPaymentsByUserId(User user,
                                                 Long userId, Pageable pageable);

    PaymentResponseDto renewExistingPayment(Long userId, Long rentalId);

    PaymentResponseDto fulfillPayment(String sessionId);

    String handleCancel();

    void processWebhook(String payload, String sigHeader);
}
