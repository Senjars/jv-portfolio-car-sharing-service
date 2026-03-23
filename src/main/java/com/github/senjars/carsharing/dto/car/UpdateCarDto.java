package com.github.senjars.carsharing.dto.car;

import com.github.senjars.carsharing.model.car.TypeName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdateCarDto(
        @NotNull(message = "Car type cannot be null")
        TypeName type,

        String brand,

        String model,

        @PositiveOrZero(message = "Inventory cannot be negative")
        int inventory,

        @Positive(message = "Daily fee must be greater than zero")
        BigDecimal dailyFee
) {
}
