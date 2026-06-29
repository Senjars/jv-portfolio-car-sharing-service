package com.github.senjars.carsharing.mapper;

import com.github.senjars.carsharing.dto.payment.PaymentResponseDto;
import com.github.senjars.carsharing.model.payment.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "rentalId", source = "rental.id")
    PaymentResponseDto toDto(Payment payment);
}
