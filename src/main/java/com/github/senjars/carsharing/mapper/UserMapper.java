package com.github.senjars.carsharing.mapper;

import com.github.senjars.carsharing.dto.user.UpdateUserInfoDto;
import com.github.senjars.carsharing.dto.user.UpdateUserRoleDto;
import com.github.senjars.carsharing.dto.user.UserDto;
import com.github.senjars.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.senjars.carsharing.model.user.Role;
import com.github.senjars.carsharing.model.user.RoleName;
import com.github.senjars.carsharing.model.user.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    User toEntity(UserRegistrationRequestDto requestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserDto(UpdateUserInfoDto updateUserInfoDto, @MappingTarget User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "firstName", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    @Mapping(target = "roles", source = "roleNames")
    @Mapping(target = "authorities", ignore = true)
    void updateUserRoleDto(UpdateUserRoleDto updateUserRoleDto, @MappingTarget User user);

    default RoleName map(Role role) {
        return (role == null) ? null : role.getName();
    }

    default Role map(RoleName roleName) {
        if (roleName == null) {
            return null;
        }
        Role role = new Role();
        role.setName(roleName);
        return role;
    }
}
