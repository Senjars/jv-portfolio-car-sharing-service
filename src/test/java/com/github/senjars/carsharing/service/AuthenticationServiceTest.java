package com.github.senjars.carsharing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.senjars.carsharing.dto.user.UserDto;
import com.github.senjars.carsharing.dto.user.UserLoginRequestDto;
import com.github.senjars.carsharing.dto.user.UserLoginResponseDto;
import com.github.senjars.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.senjars.carsharing.exception.RegistrationException;
import com.github.senjars.carsharing.mapper.UserMapper;
import com.github.senjars.carsharing.model.user.Role;
import com.github.senjars.carsharing.model.user.RoleName;
import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.repository.RoleRepository;
import com.github.senjars.carsharing.repository.UserRepository;
import com.github.senjars.carsharing.security.AuthenticationService;
import com.github.senjars.carsharing.security.JwtUtil;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("Login - Should return token record when credentials are valid")
    void login_ValidCredentials_ReturnsToken() {
        // GIVEN
        UserLoginRequestDto requestDto = new UserLoginRequestDto("test@test.com", "password");
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@test.com");
        when(jwtUtil.generateToken("test@test.com")).thenReturn("mocked-jwt-token");

        // WHEN
        UserLoginResponseDto response = authenticationService.login(requestDto);

        // THEN
        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.token());
    }

    @Test
    @DisplayName("Register - Should save user and return UserDto record when data is valid")
    void register_ValidRequest_SavesUserAndReturnsDto() throws RegistrationException {
        // GIVEN
        UserRegistrationRequestDto requestDto = new UserRegistrationRequestDto(
                "new@test.com", "pass123", "pass123", "John", "Doe"
        );

        User user = new User();
        User savedUser = new User();
        Role role = new Role();
        role.setName(RoleName.CUSTOMER);

        UserDto expectedDto = new UserDto(
                1L,
                "new@test.com",
                "John",
                "Doe",
                Set.of(RoleName.CUSTOMER)
        );

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toEntity(requestDto)).thenReturn(user);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("pass123")).thenReturn("encoded_pass");
        when(userRepository.save(user)).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(expectedDto);

        // WHEN
        UserDto result = authenticationService.register(requestDto);

        // THEN
        assertNotNull(result);
        assertEquals("new@test.com", result.email());
        assertEquals("John", result.firstName());
        verify(passwordEncoder).encode("pass123");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Register - Should throw RegistrationException when email already exists")
    void register_EmailAlreadyExists_ThrowsException() {
        // GIVEN
        UserRegistrationRequestDto requestDto = new UserRegistrationRequestDto(
                "exists@test.com", "p", "p", "J", "D"
        );

        when(userRepository.existsByEmail("exists@test.com")).thenReturn(true);

        // WHEN & THEN
        assertThrows(RegistrationException.class,
                () -> authenticationService.register(requestDto));
    }
}