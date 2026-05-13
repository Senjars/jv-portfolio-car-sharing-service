package com.github.senjars.carsharing.dto.payment;

import com.github.senjars.carsharing.model.payment.PaymentType;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequestDto(
        @NotNull Long rentalId,
        @NotNull PaymentType type
) {
}
