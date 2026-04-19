package com.github.senjars.carsharing.mapper;

import com.github.senjars.carsharing.dto.payment.PaymentResponseDto;
import com.github.senjars.carsharing.model.payment.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponseDto toDto(Payment payment);

    Payment toEntity(PaymentResponseDto paymentResponseDto);
}
