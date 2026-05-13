package com.github.senjars.carsharing.service.impl;

import com.github.senjars.carsharing.dto.user.UpdateUserInfoDto;
import com.github.senjars.carsharing.dto.user.UpdateUserRoleDto;
import com.github.senjars.carsharing.dto.user.UserDto;
import com.github.senjars.carsharing.exception.EntityNotFoundException;
import com.github.senjars.carsharing.mapper.UserMapper;
import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.repository.UserRepository;
import com.github.senjars.carsharing.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto updateRole(Long userId, UpdateUserRoleDto updateUserRoleDto) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new EntityNotFoundException("User with id " + userId + " not found"));

        userMapper.updateUserRoleDto(updateUserRoleDto, user);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserInfo(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new EntityNotFoundException("User with id " + userId + " not found"));

        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto updateUserInfo(Long userId, UpdateUserInfoDto updateUserInfoDto) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new EntityNotFoundException("User with id " + userId + " not found"));

        userMapper.updateUserDto(updateUserInfoDto, user);
        return userMapper.toDto(userRepository.save(user));
    }
}
