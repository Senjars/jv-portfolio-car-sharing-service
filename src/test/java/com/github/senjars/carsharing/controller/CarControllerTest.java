package com.github.senjars.carsharing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.senjars.carsharing.config.SecurityConfig;
import com.github.senjars.carsharing.dto.car.CarDto;
import com.github.senjars.carsharing.dto.car.CreateCarDto;
import com.github.senjars.carsharing.dto.car.UpdateCarDto;
import com.github.senjars.carsharing.exception.EntityNotFoundException;
import com.github.senjars.carsharing.exception.GlobalExceptionHandler;
import com.github.senjars.carsharing.model.car.TypeName;
import com.github.senjars.carsharing.security.CustomUserDetailsService;
import com.github.senjars.carsharing.security.JwtUtil;
import com.github.senjars.carsharing.service.CarService;
import java.math.BigDecimal;
import java.util.List;
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

@WebMvcTest(value = CarController.class,
            excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
public class CarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CarService carService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Should successfully add a new car when user has Manager role")
    @WithMockUser(username = "testUser", roles = "MANAGER")
    void addCar_validRequest_returnsCarDto() throws Exception {
        // GIVEN
        CreateCarDto createCarDto = createCreateCarDto();
        CarDto expectedCarDto = createCarDto();

        // WHEN
        when(carService.addCar(any(CreateCarDto.class))).thenReturn(expectedCarDto);

        // THEN
        mockMvc.perform(post("/api/cars")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCarDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.brand").value("Brand"))
                .andExpect(jsonPath("$.model").value("Model"));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when unauthenticated user tries to add a car")
    void addCar_unauthenticatedUser_returnsForbidden() throws Exception {
        // GIVEN
        CreateCarDto createCarDto = createCreateCarDto();

        // WHEN & THEN
        mockMvc.perform(post("/api/cars")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCarDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when user without Manager permissions tries to add a car")
    @WithMockUser(username = "testUser", roles = "CUSTOMER")
    void addCar_insufficientPermissions_returnsForbidden() throws Exception {
        // GIVEN
        CreateCarDto createCarDto = createCreateCarDto();

        // WHEN & THEN
        mockMvc.perform(post("/api/cars")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCarDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should successfully remove a car when user is a Manager")
    @WithMockUser(username = "testUser", roles = "MANAGER")
    void removeCar_validRequest_returnsNoContent() throws Exception {
        // WHEN & THEN
        mockMvc.perform(delete("/api/cars/{carId}", 1L)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(carService).removeCar(1L);
    }

    @Test
    @DisplayName("Should return 403 Forbidden when unauthenticated user tries to remove a car")
    void removeCar_unauthenticatedUser_returnsForbidden() throws Exception {
        // WHEN & THEN
        mockMvc.perform(delete("/api/cars/{carId}", 1L)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when a Customer tries to remove a car")
    @WithMockUser(username = "testUser", roles = "CUSTOMER")
    void removeCar_insufficientPermissions_returnsForbidden() throws Exception {
        // WHEN & THEN
        mockMvc.perform(delete("/api/cars/{carId}", 1L)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to remove a non-existent car")
    @WithMockUser(username = "testUser", roles = "MANAGER")
    void removeCar_nonExistentCar_returnsNotFound() throws Exception {
        // WHEN
        doThrow(new EntityNotFoundException("Car not found"))
                .when(carService).removeCar(1L);

        // THEN
        mockMvc.perform(delete("/api/cars/{carId}", 1L)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should successfully update car details when user is a Manager")
    @WithMockUser(username = "testUser", roles = "MANAGER")
    void updateCar_validRequest_returnsUpdatedCarDto() throws Exception {
        // GIVEN
        UpdateCarDto updateCarDto = createUpdateCarDto();
        CarDto expectedCarDto = createCarDto();

        // WHEN
        when(carService.updateCar(eq(1L), any(UpdateCarDto.class))).thenReturn(expectedCarDto);

        // THEN
        mockMvc.perform(patch("/api/cars/{carId}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCarDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("Brand"))
                .andExpect(jsonPath("$.model").value("Model"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to update a car that doesn't exist")
    @WithMockUser(username = "testUser", roles = "MANAGER")
    void updateCar_nonExistentCar_returnsNotFound() throws Exception {
        // GIVEN
        UpdateCarDto updateCarDto = createUpdateCarDto();

        // WHEN
        when(carService.updateCar(eq(1L), any(UpdateCarDto.class)))
                .thenThrow(new EntityNotFoundException("Car not found"));

        // THEN
        mockMvc.perform(patch("/api/cars/{carId}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCarDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when unauthenticated user tries to update a car")
    void updateCar_unauthenticatedUser_returnsForbidden() throws Exception {
        // GIVEN
        UpdateCarDto updateCarDto = createUpdateCarDto();

        // WHEN & THEN
        mockMvc.perform(patch("/api/cars/{carId}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCarDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when a Customer tries to update a car")
    @WithMockUser(username = "testUser", roles = "CUSTOMER")
    void updateCar_insufficientPermissions_returnsForbidden() throws Exception {
        // GIVEN
        UpdateCarDto updateCarDto = createUpdateCarDto();

        // WHEN & THEN
        mockMvc.perform(patch("/api/cars/{carId}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCarDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return a paginated list of cars for any authenticated user")
    @WithMockUser(username = "testUser")
    void getCars_validRequest_returnsPageOfCars() throws Exception {
        // GIVEN
        Pageable pageable = PageRequest.of(0, 10);
        CarDto carDto = createCarDto();
        Page<CarDto> carPage = new PageImpl<>(List.of(carDto), pageable, 1);

        // WHEN
        when(carService.getCars(any(Pageable.class))).thenReturn(carPage);

        // THEN
        mockMvc.perform(get("/api/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].brand").value("Brand"))
                .andExpect(jsonPath("$.content[0].model").value("Model"));
    }

    @Test
    @DisplayName("Should return specific car information for a valid car ID")
    @WithMockUser(username = "testUser")
    void getCarInfo_validRequest_returnsCarDto() throws Exception {
        // GIVEN
        CarDto expectedCarDto = createCarDto();

        // WHEN
        when(carService.getCarInfo(1L)).thenReturn(expectedCarDto);

        // THEN
        mockMvc.perform(get("/api/cars/{carId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.brand").value("Brand"))
                .andExpect(jsonPath("$.model").value("Model"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when requesting info for a non-existent car")
    @WithMockUser(username = "testUser")
    void getCarInfo_nonExistentCar_returnsNotFound() throws Exception {
        // WHEN
        when(carService.getCarInfo(999L))
                .thenThrow(new EntityNotFoundException("Car not found"));

        // THEN
        mockMvc.perform(get("/api/cars/{carId}", 999L))
                .andExpect(status().isNotFound());
    }

    private CarDto createCarDto() {
        return new CarDto(
                1L,
                TypeName.SEDAN,
                "Brand",
                "Model",
                1,
                BigDecimal.valueOf(100));
    }

    private CreateCarDto createCreateCarDto() {
        return new CreateCarDto(TypeName.SEDAN,
                "Brand",
                "Model",
                1,
                BigDecimal.valueOf(100));
    }

    private UpdateCarDto createUpdateCarDto() {
        return new UpdateCarDto(
                TypeName.SEDAN,
                "Brand",
                "Model",
                1,
                BigDecimal.valueOf(100));
    }
}