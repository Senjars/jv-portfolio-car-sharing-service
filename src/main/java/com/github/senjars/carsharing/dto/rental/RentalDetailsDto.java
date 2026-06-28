package com.github.senjars.carsharing.dto.rental;

import com.github.senjars.carsharing.dto.car.CarShortDto;
import java.time.LocalDate;

public record RentalDetailsDto(
        Long id,
        CarShortDto car,
        Long userId,
        LocalDate rentalDate,
        LocalDate returnDate,
        LocalDate actualReturnDate
) {
}
