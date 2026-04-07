package com.github.senjars.carsharing.controller;

import com.github.senjars.carsharing.dto.rental.CreateRentalRequestDto;
import com.github.senjars.carsharing.dto.rental.RentalDto;
import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.service.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalService rentalService;

    @PostMapping
    @Operation(
            summary = "Rent a car",
            description = "Creates a new rental for a car",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Car rented successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "404", description = "Car not found")
            }
    )
    public RentalDto rentCar(@AuthenticationPrincipal User user,
                             @Valid @RequestBody CreateRentalRequestDto requestDto) {
        return rentalService.rentCar(user.getId(), requestDto);
    }

    @PostMapping("/return")
    @Operation(
            summary = "Return a car",
            description = "Returns a rented car",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Car returned successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "404", description = "Rental not found")
            }
    )
    public RentalDto returnCar(@AuthenticationPrincipal User user,
                               @Valid Long rentalId) {
        return rentalService.returnCar(user.getId(), rentalId);
    }

    @GetMapping
    @Operation(
            summary = "Get all rentals",
            description = "Retrieves a page of rentals",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Rentals retrieved successfully"),
                    @ApiResponse(responseCode = "400",
                            description = "Bad request")
            }
    )
    public Page<RentalDto> getRentalsByUserIdAndStatus(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {

        Long targetUserId = isManager(user) ? userId : user.getId();

        return rentalService.getRentalsByUserIdAndStatus(targetUserId, isActive, pageable);
    }

    @GetMapping("/{rentalId}")
    @Operation(
            summary = "Get rental by ID",
            description = "Retrieves a rental by its ID",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Rental retrieved successfully"),
                    @ApiResponse(responseCode = "400",
                            description = "Bad request"),
                    @ApiResponse(responseCode = "404",
                            description = "Rental not found")
            }
    )
    public RentalDto getRentalById(@AuthenticationPrincipal User user,
                                   @PathVariable Long rentalId) {
        return rentalService.getRentalById(rentalId, user.getId(), isManager(user));
    }

    private boolean isManager(User user) {
        return user.getAuthorities().stream().anyMatch(
                a -> a.getAuthority().equals("MANAGER"));
    }
}
