package com.github.senjars.carsharing.dto.car;

import java.math.BigDecimal;

public record CarShortDto(
        Long id,
        String type,
        String brand,
        String model,
        BigDecimal dailyFee
) {
}
