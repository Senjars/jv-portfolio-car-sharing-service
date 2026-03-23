package com.github.senjars.carsharing.controller;

import com.github.senjars.carsharing.dto.user.UpdateUserInfoDto;
import com.github.senjars.carsharing.dto.user.UpdateUserRoleDto;
import com.github.senjars.carsharing.dto.user.UserDto;
import com.github.senjars.carsharing.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{userId}/role")
    @Operation(
            summary = "Update user role",
            description = "Updates the role of a user",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "User role updated successfully"),
                    @ApiResponse(responseCode = "404",
                            description = "User not found")
            }
    )
    public UserDto updateRole(@PathVariable Long userId,
                              @Valid @RequestBody UpdateUserRoleDto updateUserRoleDto) {
        return userService.updateRole(userId, updateUserRoleDto);
    }

    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/me")
    @Operation(
            summary = "Update user information",
            description = "Updates information about user",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "User information updated successfully"),
                    @ApiResponse(responseCode = "404",
                            description = "User not found")
            }
    )
    public UserDto updateUserInfo(Long userId,
                                  @Valid @RequestBody UpdateUserInfoDto updateUserInfoDto) {
        return userService.updateUserInfo(userId, updateUserInfoDto);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/me")
    @Operation(
            summary = "Get user information",
            description = "Retrieves user information",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "User information retrieved successfully"),
                    @ApiResponse(responseCode = "404",
                            description = "User not found")
            }
    )
    public UserDto getUserInfo(Long userId) {
        return userService.getUserInfo(userId);
    }
}
