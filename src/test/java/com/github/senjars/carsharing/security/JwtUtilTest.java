package com.github.senjars.carsharing.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.github.senjars.carsharing.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @Mock
    private JwtConfig jwtConfig;

    private final String secret = "super_secret_key_for_testing_purposes_must_be_long_enough";
    private final long expiration = 3600000; // 1h
    private final String username = "test@example.com";

    @BeforeEach
    void setUp() {
        when(jwtConfig.getSecret()).thenReturn(secret);
        when(jwtConfig.getExpiration()).thenReturn(expiration);
        jwtUtil = new JwtUtil(jwtConfig);
    }

    @Test
    @DisplayName("Should generate a non-empty token for a given username")
    void generateToken_ValidUsername_ReturnsToken() {
        String token = jwtUtil.generateToken(username);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should return true for a valid, newly generated token")
    void isValidToken_ValidToken_ReturnsTrue() {
        String token = jwtUtil.generateToken(username);

        boolean isValid = jwtUtil.isValidToken(token);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should extract the correct username from the token")
    void getUsernameFromToken_ValidToken_ReturnsCorrectUsername() {
        String token = jwtUtil.generateToken(username);

        String extractedUsername = jwtUtil.getUsernameFromToken(token);

        assertEquals(username, extractedUsername);
    }

    @Test
    @DisplayName("Should return false when token signature is invalid")
    void isValidToken_InvalidSignature_ReturnsFalse() {
        String token = jwtUtil.generateToken(username);
        String tamperedToken = token + "modified";

        boolean isValid = jwtUtil.isValidToken(tamperedToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false when token is expired")
    void isValidToken_ExpiredToken_ReturnsFalse() {
        when(jwtConfig.getSecret()).thenReturn(secret);
        when(jwtConfig.getExpiration()).thenReturn(-1000L);
        JwtUtil expiredJwtUtil = new JwtUtil(jwtConfig);

        String token = expiredJwtUtil.generateToken(username);

        boolean isValid = expiredJwtUtil.isValidToken(token);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for a malformed token")
    void isValidToken_MalformedToken_ReturnsFalse() {
        String malformedToken = "not.a.valid.jwt.token";

        boolean isValid = jwtUtil.isValidToken(malformedToken);

        assertFalse(isValid);
    }
}