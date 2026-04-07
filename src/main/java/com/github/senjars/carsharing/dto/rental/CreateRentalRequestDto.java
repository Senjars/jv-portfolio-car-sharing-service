package com.github.senjars.carsharing.dto.rental;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateRentalRequestDto(
        @NotNull(message = "Rental date is required")
        @FutureOrPresent(message = "Rental date cannot be in the past")
        LocalDate rentalDate,

        @NotNull(message = "Return date is required")
        @Future(message = "Return date must be in the future")
        LocalDate returnDate,

        @NotNull(message = "Car ID is required")
        Long carId
) {
}
