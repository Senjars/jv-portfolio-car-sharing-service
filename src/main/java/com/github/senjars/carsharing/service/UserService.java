package com.github.senjars.carsharing.service;

import com.github.senjars.carsharing.dto.user.UpdateUserInfoDto;
import com.github.senjars.carsharing.dto.user.UpdateUserRoleDto;
import com.github.senjars.carsharing.dto.user.UserDto;

public interface UserService {

    UserDto updateRole(Long userId, UpdateUserRoleDto updateUserRoleDto);

    UserDto getUserInfo(Long userId);

    UserDto updateUserInfo(Long userId, UpdateUserInfoDto updateUserInfoDto);
}
