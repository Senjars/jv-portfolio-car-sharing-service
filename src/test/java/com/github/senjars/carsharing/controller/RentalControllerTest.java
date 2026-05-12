package com.github.senjars.carsharing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.senjars.carsharing.config.SecurityConfig;
import com.github.senjars.carsharing.dto.rental.CreateRentalRequestDto;
import com.github.senjars.carsharing.dto.rental.RentalDto;
import com.github.senjars.carsharing.model.user.Role;
import com.github.senjars.carsharing.model.user.RoleName;
import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.notify.RentalNotificationScheduler;
import com.github.senjars.carsharing.security.CustomUserDetailsService;
import com.github.senjars.carsharing.security.JwtUtil;
import com.github.senjars.carsharing.service.RentalService;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = RentalController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
public class RentalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RentalService rentalService;

    @MockitoBean
    private RentalNotificationScheduler rentalNotificationScheduler;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Should successfully create a rental for a car with valid dates")
    void rentCar_validRequest_returnsRentalDto() throws Exception {
        // GIVEN
        User mockUser = createMockUserWithId();
        CreateRentalRequestDto requestDto = new CreateRentalRequestDto(
                1L, LocalDate.now(), LocalDate.now().plusDays(3));
        RentalDto expectedDto = createRentalDto();

        // WHEN
        when(rentalService.rentCar(anyLong(), any(CreateRentalRequestDto.class)))
                .thenReturn(expectedDto);

        // THEN
        mockMvc.perform(post("/api/rentals")
                        .with(csrf())
                        .with(user(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.carId").value(1L));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when unauthenticated user tries to rent a car")
    void rentCar_unauthenticatedUser_throwsForbidden() throws Exception {
        // GIVEN
        CreateRentalRequestDto requestDto = new CreateRentalRequestDto(
                1L, LocalDate.now(), LocalDate.now().plusDays(3));

        // THEN
        mockMvc.perform(post("/api/rentals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should successfully process a car return and close the rental")
    void returnCar_validRequest_returnsRentalDto() throws Exception {
        // GIVEN
        User mockUser = createMockUserWithId();
        Long rentalId = 1L;
        RentalDto expectedDto = createRentalDto();

        // WHEN
        when(rentalService.returnCar(anyLong(), eq(rentalId))).thenReturn(expectedDto);

        // THEN
        mockMvc.perform(post("/api/rentals/return")
                        .with(csrf())
                        .with(user(mockUser))
                        .param("rentalId", rentalId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when unauthenticated user tries to return a car")
    void returnCar_unauthenticatedUser_throwsForbidden() throws Exception {
        // GIVEN
        Long rentalId = 1L;

        // THEN
        mockMvc.perform(post("/api/rentals/return")
                        .with(csrf())
                        .param("rentalId", rentalId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return a paginated list of rentals filtered by activity status")
    void getRentalsByUserIdAndStatus_validRequest_returnsPageOfRentals() throws Exception {
        // GIVEN
        User mockUser = createMockUserWithId();
        Pageable pageable = PageRequest.of(0, 10);
        RentalDto rentalDto = createRentalDto();
        Page<RentalDto> rentalPage = new PageImpl<>(List.of(rentalDto), pageable, 1);

        // WHEN
        when(rentalService.getRentalsByUserIdAndStatus(anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(rentalPage);

        // THEN
        mockMvc.perform(get("/api/rentals")
                        .param("isActive", "true")
                        .with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    @DisplayName("Should return detailed information about a specific rental by its ID")
    void getRentalById_validRequest_returnsRentalDto() throws Exception {
        // GIVEN
        Long rentalId = 1L;
        User mockUser = createMockUserWithId();
        RentalDto expectedDto = createRentalDto();

        // WHEN
        when(rentalService.getRentalById(eq(rentalId), anyLong(), anyBoolean()))
                .thenReturn(expectedDto);

        // THEN
        mockMvc.perform(get("/api/rentals/{rentalId}", rentalId)
                        .with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.carId").value(1L));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when unauthenticated user tries to access rental details")
    void getRentalById_unauthenticatedUser_throwsForbidden() throws Exception {
        // GIVEN
        Long rentalId = 1L;

        // THEN
        mockMvc.perform(get("/api/rentals/{rentalId}", rentalId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow Manager to manually trigger the overdue rental check")
    @WithMockUser(username = "testUser", roles = "MANAGER")
    void triggerOverdueCheck_managerUser_returnsOk() throws Exception {
        // WHEN & THEN
        mockMvc.perform(post("/api/rentals/trigger-overdue-check")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(rentalNotificationScheduler).checkOverdueRentals();
    }

    @Test
    @DisplayName("Should return 403 Forbidden when a Customer tries to trigger the overdue check")
    void triggerOverdueCheck_customerUser_returnsForbidden() throws Exception {
        // GIVEN
        User mockUser = createMockUserWithId();

        // THEN
        mockMvc.perform(post("/api/rentals/trigger-overdue-check")
                        .with(csrf())
                        .with(user(mockUser)))
                .andExpect(status().isForbidden());
    }

    private RentalDto createRentalDto() {
        return new RentalDto(
                1L, 1L, 1L,
                LocalDate.now(), LocalDate.now().plusDays(3),
                null
        );
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
}