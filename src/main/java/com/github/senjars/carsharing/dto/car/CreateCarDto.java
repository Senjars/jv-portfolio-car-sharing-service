package com.github.senjars.carsharing.dto.car;

import com.github.senjars.carsharing.model.car.TypeName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateCarDto(
        @NotNull(message = "Car type is required")
        TypeName type,

        @NotBlank(message = "Brand cannot be blank")
        String brand,

        @NotBlank(message = "Model cannot be blank")
        String model,

        @PositiveOrZero(message = "Inventory cannot be negative")
        int inventory,

        @Positive(message = "Daily fee must be greater than zero")
        BigDecimal dailyFee
) {
}
