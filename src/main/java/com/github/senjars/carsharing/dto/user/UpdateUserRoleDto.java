package com.github.senjars.carsharing.dto.user;

import com.github.senjars.carsharing.model.user.RoleName;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleDto(
        @NotNull(message = "Role name cannot be null")
        RoleName roleName
) {
}
