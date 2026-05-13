package com.github.senjars.carsharing.dto.car;

import com.github.senjars.carsharing.model.car.TypeName;
import java.math.BigDecimal;

public record CarDto(
        Long id,
        TypeName type,
        String brand,
        String model,
        int inventory,
        BigDecimal dailyFee
) {
}
