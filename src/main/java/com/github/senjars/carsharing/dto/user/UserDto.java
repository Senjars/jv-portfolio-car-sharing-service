package com.github.senjars.carsharing.dto.user;

import com.github.senjars.carsharing.model.user.RoleName;

public record UserDto(
        String email,
        String firstName,
        String lastName,
        RoleName roleName
) {
}
