package com.github.senjars.carsharing.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRegistrationRequestDto(

        @Email
        @NotBlank
        String email,

        @NotBlank
        String password,

        @NotBlank
        String repeatPassword,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName
) {
}
