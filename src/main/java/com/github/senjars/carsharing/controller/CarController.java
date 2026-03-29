package com.github.senjars.carsharing.controller;

import com.github.senjars.carsharing.dto.car.CarDto;
import com.github.senjars.carsharing.dto.car.CreateCarDto;
import com.github.senjars.carsharing.dto.car.UpdateCarDto;
import com.github.senjars.carsharing.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarService carService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @Operation(
            summary = "Add a new car",
            description = "Adds a new car to the database",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Car added successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad request")
            }
    )
    @PreAuthorize("hasRole('MANAGER')")
    public CarDto addCar(@Valid @RequestBody CreateCarDto createCarDto) {
        return carService.addCar(createCarDto);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{carId}")
    @Operation(
            summary = "Remove a single car",
            description = "Removes a single car from the database",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Car removed successfully"),
                    @ApiResponse(responseCode = "404", description = "Car not found")
            }
    )
    @PreAuthorize("hasRole('MANAGER')")
    public void removeCar(@PathVariable Long carId) {
        carService.removeCar(carId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/{carId}")
    @Operation(
            summary = "Update car information",
            description = "Updates car information",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Car updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Car not found")
            }
    )
    @PreAuthorize("hasRole('MANAGER')")
    public CarDto updateCar(@PathVariable Long carId,
                            @Valid @RequestBody UpdateCarDto updateCarDto) {
        return carService.updateCar(carId, updateCarDto);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    @Operation(
            summary = "Get all cars",
            description = "Retrieves a page of cars",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cars retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "No cars found")
            }
    )
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    public Page<CarDto> getCars(Pageable pageable) {
        return carService.getCars(pageable);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{carId}")
    @Operation(
            summary = "Get car information",
            description = "Returns all information about a single car",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Car retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Car not found")
            }
    )
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    public CarDto getCarInfo(@PathVariable Long carId) {
        return carService.getCarInfo(carId);
    }
}
