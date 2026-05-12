package com.github.senjars.carsharing.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.senjars.carsharing.dto.user.UpdateUserInfoDto;
import com.github.senjars.carsharing.dto.user.UpdateUserRoleDto;
import com.github.senjars.carsharing.dto.user.UserDto;
import com.github.senjars.carsharing.exception.EntityNotFoundException;
import com.github.senjars.carsharing.mapper.UserMapper;
import com.github.senjars.carsharing.model.user.RoleName;
import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.repository.UserRepository;
import com.github.senjars.carsharing.service.impl.UserServiceImpl;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Test
    @DisplayName("Should successfully update user security roles based on provided DTO")
    void updateRole_validRole_updatesRole() {
        // GIVEN
        User user = createUser();
        UpdateUserRoleDto updateUserRoleDto = new UpdateUserRoleDto(Set.of(RoleName.MANAGER));

        // WHEN
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.updateRole(user.getId(), updateUserRoleDto);

        // THEN
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should successfully retrieve user profile information for a valid user ID")
    void getUserInfo_validId_returnsUserInfo() {
        // GIVEN
        User user = createUser();
        UserDto expectedDto = createUserDto();

        // WHEN
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expectedDto);

        UserDto actualDto = userService.getUserInfo(user.getId());

        // THEN
        Assertions.assertEquals(expectedDto, actualDto);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when attempting to fetch profile for a non-existent user")
    void getUserInfo_invalidId_throwsEntityNotFoundException() {
        // GIVEN
        Long invalidId = 999L;

        // WHEN
        when(userRepository.findById(invalidId)).thenReturn(Optional.empty());

        // THEN
        Assertions.assertThrows(EntityNotFoundException.class,
                () -> userService.getUserInfo(invalidId));
    }

    @Test
    @DisplayName("Should successfully update user personal details and return the updated profile DTO")
    void updateUserInfo_validId_returnsUpdatedUserInfo() {
        // GIVEN
        User user = createUser();
        UpdateUserInfoDto updateUserInfoDto = createUpdateUserInfoDto();
        UserDto expectedDto = createUserDto();

        // WHEN
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expectedDto);

        UserDto actualDto = userService.updateUserInfo(user.getId(), updateUserInfoDto);

        // THEN
        Assertions.assertEquals(expectedDto.firstName(), actualDto.firstName());
        Assertions.assertEquals(expectedDto.lastName(), actualDto.lastName());
        Assertions.assertEquals(expectedDto.email(), actualDto.email());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when trying to update information for a non-existent user ID")
    void updateUserInfo_invalidId_throwsEntityNotFoundException() {
        // GIVEN
        Long invalidId = 999L;

        // WHEN
        when(userRepository.findById(invalidId)).thenReturn(Optional.empty());

        // THEN
        Assertions.assertThrows(EntityNotFoundException.class,
                () -> userService.updateUserInfo(invalidId, null));
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setPassword("password");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("test@test.com");
        return user;
    }

    private UserDto createUserDto() {
        return new UserDto(
                1L,
                "test@test.com",
                "John",
                "Doe",
                Set.of(RoleName.CUSTOMER));
    }

    private UpdateUserInfoDto createUpdateUserInfoDto() {
        return new UpdateUserInfoDto(
                "Jessica",
                "Smith",
                "test@test.com");
    }

}
