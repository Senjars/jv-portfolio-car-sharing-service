package com.github.senjars.carsharing.controller;

import com.github.senjars.carsharing.dto.user.UserDto;
import com.github.senjars.carsharing.dto.user.UserLoginRequestDto;
import com.github.senjars.carsharing.dto.user.UserLoginResponseDto;
import com.github.senjars.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.senjars.carsharing.exception.RegistrationException;
import com.github.senjars.carsharing.security.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates a user with email and password",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "User logged in successfully"),
                    @ApiResponse(responseCode = "401",
                            description = "Invalid credentials"),
                    @ApiResponse(responseCode = "400",
                            description = "Bad request")
            }
    )
    public UserLoginResponseDto login(@RequestBody @Valid UserLoginRequestDto requestDto) {
        return authenticationService.login(requestDto);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    @Operation(
            summary = "User registration",
            description = "Registers a new user in the system",
            responses = {
                    @ApiResponse(responseCode = "201",
                            description = "User registered successfully"),
                    @ApiResponse(responseCode = "400",
                            description = "Bad request")
            }
    )
    public UserDto register(@RequestBody @Valid UserRegistrationRequestDto requestDto)
            throws RegistrationException {
        return authenticationService.register(requestDto);
    }
}
