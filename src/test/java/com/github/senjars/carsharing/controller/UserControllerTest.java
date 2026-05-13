package com.github.senjars.carsharing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.senjars.carsharing.config.SecurityConfig;
import com.github.senjars.carsharing.dto.user.UpdateUserInfoDto;
import com.github.senjars.carsharing.dto.user.UpdateUserRoleDto;
import com.github.senjars.carsharing.dto.user.UserDto;
import com.github.senjars.carsharing.exception.EntityNotFoundException;
import com.github.senjars.carsharing.exception.GlobalExceptionHandler;
import com.github.senjars.carsharing.model.user.Role;
import com.github.senjars.carsharing.model.user.RoleName;
import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.repository.UserRepository;
import com.github.senjars.carsharing.security.CustomUserDetailsService;
import com.github.senjars.carsharing.security.JwtUtil;
import com.github.senjars.carsharing.service.UserService;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = {UserController.class, GlobalExceptionHandler.class},
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Should successfully update user roles when performed by a Manager")
    @WithMockUser(username = "testUser", roles = "MANAGER")
    void updateRole_validRequest_returnsUserDto() throws Exception {
        // GIVEN
        UpdateUserRoleDto updateUserRoleDto = new UpdateUserRoleDto(Set.of(RoleName.MANAGER));
        UserDto expectedUserDto = new UserDto(
                1L, "test@test.com", "John", "Doe", Set.of(RoleName.MANAGER));

        // WHEN
        when(userService.updateRole(anyLong(), any(UpdateUserRoleDto.class)))
                .thenReturn(expectedUserDto);

        // THEN
        mockMvc.perform(put("/api/users/{userId}/role", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRoleDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when unauthenticated user tries to update roles")
    void updateRole_unauthenticatedUser_throwsForbidden() throws Exception {
        // GIVEN
        UpdateUserRoleDto updateUserRoleDto = new UpdateUserRoleDto(Set.of(RoleName.MANAGER));

        // THEN
        mockMvc.perform(put("/api/users/{userId}/role", 1L)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRoleDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when a Customer tries to update user roles")
    @WithMockUser(username = "testUser", roles = "CUSTOMER")
    void updateRole_insufficientPermissions_throwsForbidden() throws Exception {
        // GIVEN
        UpdateUserRoleDto updateUserRoleDto = new UpdateUserRoleDto(Set.of(RoleName.MANAGER));

        // THEN
        mockMvc.perform(put("/api/users/{userId}/role", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRoleDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should successfully update personal profile information for the current user")
    void updateUserInfo_validRequest_returnsUpdatedUserDto() throws Exception {
        // GIVEN
        User mockUser = createMockUserWithId();
        UpdateUserInfoDto updateUserInfoDto =
                new UpdateUserInfoDto("James", "Doe", "updated@test.com");

        UserDto expectedDto = new UserDto(
                1L, "test@test.com",
                "James", "Doe",
                Set.of(RoleName.CUSTOMER));

        // WHEN
        when(userService.updateUserInfo(anyLong(), any(UpdateUserInfoDto.class)))
                .thenReturn(expectedDto);

        // THEN
        mockMvc.perform(patch("/api/users/me")
                        .with(csrf())
                        .with(user(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserInfoDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("James"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to update info for a non-existent user")
    void updateUserInfo_validRequestButUserNotFound_throwsEntityNotFoundException() throws Exception {
        // GIVEN
        User mockUser = createMockUserWithId();
        UpdateUserInfoDto updateUserInfoDto =
                new UpdateUserInfoDto("James", "Smith", "test@test.com");

        // WHEN
        when(userService.updateUserInfo(anyLong(), any(UpdateUserInfoDto.class)))
                .thenThrow(new EntityNotFoundException("User not found"));

        // THEN
        mockMvc.perform(patch("/api/users/me")
                        .with(csrf())
                        .with(user(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserInfoDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should successfully retrieve profile information for the current user")
    void getUserInfo_validRequest_returnsUserInfo() throws Exception {
        // GIVEN
        User user = createMockUserWithId();
        UserDto userDto = new UserDto(
                1L, "test@test.com",
                "John", "Doe",
                Set.of(RoleName.CUSTOMER));

        // WHEN
        when(userService.getUserInfo(user.getId())).thenReturn(userDto);

        // THEN
        mockMvc.perform(get("/api/users/me")
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when unauthenticated user tries to access profile info")
    void getUserInfo_unauthenticatedUser_throwsForbidden() throws Exception {
        // THEN
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    private User createUser() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        return user;
    }

    private User createMockUserWithId() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setPassword("123Password123!");
        user.setFirstName("John");
        user.setLastName("Doe");
        RoleName roleName = RoleName.CUSTOMER;
        Role role = new Role();
        role.setName(roleName);
        user.setRoles(Set.of(role));
        return user;
    }

    private UserDto createUserDto() {
        return new UserDto(
                1L, "test@test.com",
                "James", "Doe",
                Set.of(RoleName.CUSTOMER));
    }

}
