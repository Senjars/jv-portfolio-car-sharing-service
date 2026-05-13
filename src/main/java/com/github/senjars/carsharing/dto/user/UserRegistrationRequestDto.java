package com.github.senjars.carsharing.dto.user;

import com.github.senjars.carsharing.validation.FieldMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@FieldMatch(first = "password", second = "repeatPassword", message = "Passwords must match")
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
