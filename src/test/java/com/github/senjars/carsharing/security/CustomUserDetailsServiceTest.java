package com.github.senjars.carsharing.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private final String email = "test@example.com";

    @Test
    @DisplayName("Should return UserDetails when user exists in database")
    void loadUserByUsername_UserExists_ReturnsUserDetails() {
        // GIVEN
        User user = new User();
        user.setEmail(email);
        user.setPassword("hashed_password");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // WHEN
        UserDetails result = customUserDetailsService.loadUserByUsername(email);

        // THEN
        assertNotNull(result);
        assertEquals(email, result.getUsername());
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist")
    void loadUserByUsername_UserDoesNotExist_ThrowsException() {
        // GIVEN
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // WHEN & THEN
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(email)
        );

        assertTrue(exception.getMessage().contains(email));
        verify(userRepository, times(1)).findByEmail(email);
    }
}
