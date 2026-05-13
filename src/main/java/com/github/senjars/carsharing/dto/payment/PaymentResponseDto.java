package com.github.senjars.carsharing.dto.payment;

import com.github.senjars.carsharing.model.payment.PaymentStatus;
import com.github.senjars.carsharing.model.payment.PaymentType;
import java.math.BigDecimal;

public record PaymentResponseDto(
        String id,
        String rentalId,
        PaymentStatus status,
        PaymentType type,
        BigDecimal amountToPay,
        String sessionId,
        String sessionUrl
) {
}
