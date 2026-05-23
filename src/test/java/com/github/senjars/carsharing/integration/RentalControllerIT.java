package com.github.senjars.carsharing.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.senjars.carsharing.config.TestSecurityConfig;
import com.github.senjars.carsharing.config.TestcontainersConfiguration;
import com.github.senjars.carsharing.dto.rental.CreateRentalRequestDto;
import com.github.senjars.carsharing.model.car.Car;
import com.github.senjars.carsharing.model.car.CarType;
import com.github.senjars.carsharing.model.car.TypeName;
import com.github.senjars.carsharing.model.rental.Rental;
import com.github.senjars.carsharing.model.user.Role;
import com.github.senjars.carsharing.model.user.RoleName;
import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.notify.TelegramService;
import com.github.senjars.carsharing.repository.CarRepository;
import com.github.senjars.carsharing.repository.RentalRepository;
import com.github.senjars.carsharing.repository.RoleRepository;
import com.github.senjars.carsharing.repository.UserRepository;
import com.github.senjars.carsharing.security.CustomUserDetailsService;
import com.github.senjars.carsharing.security.JwtUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@Import({TestcontainersConfiguration.class, TestSecurityConfig.class})
@ActiveProfiles("test")
public class RentalControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelegramService telegramService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        rentalRepository.deleteAll();
        userRepository.deleteAll();
        carRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully rent a car and decrease inventory")
    @WithMockUser(username = "user@example.com", roles = "CUSTOMER")
    void rentCar_ValidRequest_Success() throws Exception {
        // GIVEN
        User user = createUser();
        Car car = createCar(5);

        CreateRentalRequestDto requestDto = new CreateRentalRequestDto(
                car.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(3)
        );

        // WHEN & THEN
        mockMvc.perform(post("/api/rentals")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carId").value(car.getId()))
                .andExpect(jsonPath("$.actualReturnDate").isEmpty());

        Car updatedCar = carRepository.findById(car.getId()).orElseThrow();
        assert(updatedCar.getInventory() == 4);
    }

    @Test
    @DisplayName("Should return 400 when car inventory is 0")
    @WithMockUser(username = "user@example.com")
    void rentCar_OutStock_BadRequest() throws Exception {
        // GIVEN
        User user = createUser();
        Car car = createCar(0);

        CreateRentalRequestDto requestDto = new CreateRentalRequestDto(
                car.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(1)
        );

        // WHEN & THEN
        mockMvc.perform(post("/api/rentals")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should successfully return a car and increase inventory")
    @WithMockUser(username = "user@example.com")
    void returnCar_ValidRental_Success() throws Exception {
        // GIVEN
        User user = createUser();
        Car car = createCar(2);

        Rental rental = new Rental();
        rental.setCarId(car.getId());
        rental.setUserId(user.getId());
        rental.setRentalDate(LocalDate.now().minusDays(1));
        rental.setReturnDate(LocalDate.now().plusDays(2));
        rental = rentalRepository.save(rental);

        // WHEN & THEN
        mockMvc.perform(post("/api/rentals/return")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .param("rentalId", rental.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualReturnDate").value(LocalDate.now().toString()));

        Car updatedCar = carRepository.findById(car.getId()).orElseThrow();
        assert(updatedCar.getInventory() == 3);
    }

    @Test
    @DisplayName("Should return 400 when car is already returned")
    @WithMockUser(username = "user@example.com")
    void returnCar_AlreadyReturned_BadRequest() throws Exception {
        // GIVEN
        User user = createUser();
        Car car = createCar(1);

        Rental rental = new Rental();
        rental.setCarId(car.getId());
        rental.setUserId(user.getId());
        rental.setRentalDate(LocalDate.now().minusDays(5));
        rental.setReturnDate(LocalDate.now().minusDays(2));
        rental.setActualReturnDate(LocalDate.now().minusDays(2));
        rental = rentalRepository.save(rental);

        // WHEN & THEN
        mockMvc.perform(post("/api/rentals/return")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(user))
                        .param("rentalId", rental.getId().toString()))
                .andExpect(status().isBadRequest());
    }

    private User createUser() {
        User user = new User();
        user.setEmail("user" + System.currentTimeMillis() + "@example.com");
        user.setPassword("password");
        user.setFirstName("John");
        user.setLastName("Doe");
        RoleName roleName = RoleName.CUSTOMER;
        Role role = new Role();
        role.setName(roleName);
        roleRepository.save(role);
        user.setRoles(Set.of(role));
        return userRepository.save(user);
    }

    private Car createCar(int inventory) {
        Car car = new Car();
        car.setModel("Model S");
        car.setBrand("Tesla");
        CarType type = new CarType();
        type.setTypeName(TypeName.SEDAN);
        car.setType(type);
        car.setInventory(inventory);
        car.setDailyFee(BigDecimal.valueOf(100));
        return carRepository.save(car);
    }
}
