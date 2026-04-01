package com.github.senjars.carsharing.dto.user;

import com.github.senjars.carsharing.model.user.RoleName;
import java.util.Set;

public record UserDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        Set<RoleName> roles
) {
}
