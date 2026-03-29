package com.github.senjars.carsharing.controller;

import com.github.senjars.carsharing.dto.user.UserDto;
import com.github.senjars.carsharing.dto.user.UserLoginRequestDto;
import com.github.senjars.carsharing.dto.user.UserLoginResponseDto;
import com.github.senjars.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.senjars.carsharing.exception.RegistrationException;
import com.github.senjars.carsharing.security.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public UserLoginResponseDto login(@RequestBody @Valid UserLoginRequestDto requestDto) {
        return authenticationService.login(requestDto);
    }

    @PostMapping("/register")
    public UserDto register(@RequestBody @Valid UserRegistrationRequestDto requestDto)
            throws RegistrationException {
        return authenticationService.register(requestDto);
    }
}
