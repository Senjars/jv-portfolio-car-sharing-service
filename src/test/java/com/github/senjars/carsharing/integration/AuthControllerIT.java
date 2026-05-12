package com.github.senjars.carsharing.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.senjars.carsharing.config.TestSecurityConfig;
import com.github.senjars.carsharing.config.TestcontainersConfiguration;
import com.github.senjars.carsharing.dto.user.UserLoginRequestDto;
import com.github.senjars.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.senjars.carsharing.model.user.Role;
import com.github.senjars.carsharing.model.user.RoleName;
import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.repository.RoleRepository;
import com.github.senjars.carsharing.repository.UserRepository;
import com.github.senjars.carsharing.security.JwtUtil;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@Import({TestcontainersConfiguration.class, TestSecurityConfig.class})
@ActiveProfiles("test")
public class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        // Czyścimy bazę, żeby testy były odizolowane
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Dodajemy rolę CUSTOMER, bo serwis rejestracji jej szuka
        Role customerRole = new Role();
        customerRole.setName(RoleName.CUSTOMER);
        roleRepository.save(customerRole);

        // Jeśli Twój system ma też adminów, możesz dodać drugą
        Role adminRole = new Role();
        adminRole.setName(RoleName.MANAGER);
        roleRepository.save(adminRole);
    }

    @Test
    @DisplayName("Register - Should create user in DB and return UserDto")
    void register_ValidRequest_Success() throws Exception {
        // GIVEN
        UserRegistrationRequestDto request = new UserRegistrationRequestDto(
                "it-test@example.com",
                "securePass123",
                "securePass123",
                "John",
                "Doe"
        );

        // WHEN
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // THEN
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("it-test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"));

        User savedUser = userRepository.findByEmail("it-test@example.com").orElseThrow();
        assertTrue(passwordEncoder.matches("securePass123", savedUser.getPassword()));
    }

    @Test
    @DisplayName("Login - Should return 200 OK and JWT token")
    void login_ValidCredentials_Success() throws Exception {
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow();

        User user = new User();
        user.setEmail("login-it@test.com");
        user.setPassword(passwordEncoder.encode("secret123"));
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRoles(Set.of(customerRole));
        userRepository.save(user);

        UserLoginRequestDto loginRequest = new UserLoginRequestDto("login-it@test.com", "secret123");

        // WHEN
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // THEN
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Login - Should return 401 Unauthorized for invalid password")
    void login_InvalidPassword_Returns401() throws Exception {
        // GIVEN
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow();

        User user = new User();
        user.setEmail("login-it@test.com");
        user.setPassword(passwordEncoder.encode("secret123"));
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRoles(Set.of(customerRole));
        userRepository.save(user);

        UserLoginRequestDto wrongLoginRequest = new UserLoginRequestDto("fail@test.com", "wrong-pass");

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLoginRequest)))
                .andExpect(status().isUnauthorized());
    }
}