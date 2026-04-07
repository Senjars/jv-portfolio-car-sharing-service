package com.github.senjars.carsharing.dto.rental;

import java.time.LocalDate;

public record RentalDto(
        Long id,
        Long carId,
        Long userId,
        LocalDate rentalDate,
        LocalDate returnDate,
        LocalDate actualReturnDate
) {
}
