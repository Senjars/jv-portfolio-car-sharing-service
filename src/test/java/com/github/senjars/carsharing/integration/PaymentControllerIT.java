package com.github.senjars.carsharing.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.senjars.carsharing.config.TestSecurityConfig;
import com.github.senjars.carsharing.config.TestcontainersConfiguration;
import com.github.senjars.carsharing.dto.payment.CreatePaymentRequestDto;
import com.github.senjars.carsharing.model.car.Car;
import com.github.senjars.carsharing.model.car.CarType;
import com.github.senjars.carsharing.model.car.TypeName;
import com.github.senjars.carsharing.model.payment.PaymentType;
import com.github.senjars.carsharing.model.rental.Rental;
import com.github.senjars.carsharing.model.user.Role;
import com.github.senjars.carsharing.model.user.RoleName;
import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.notify.TelegramService;
import com.github.senjars.carsharing.repository.CarRepository;
import com.github.senjars.carsharing.repository.RentalRepository;
import com.github.senjars.carsharing.repository.RoleRepository;
import com.github.senjars.carsharing.repository.UserRepository;
import com.github.senjars.carsharing.stripe.StripeProvider;
import com.stripe.model.checkout.Session;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestSecurityConfig.class})
@Transactional
public class PaymentControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private StripeProvider stripeProvider;

    @MockitoBean
    private TelegramService telegramService;

    private Role customerRole;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.CUSTOMER);
                    return roleRepository.save(role);
                });
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create a Stripe session and save PENDING payment via REST API")
    void createPayment_ValidRequest_ShouldReturnCreated() throws Exception {
        // GIVEN
        User user = createUser("owner@test.com");
        Car car = createCar();
        Rental rental = createRental(user.getId(), car.getId());

        CreatePaymentRequestDto requestDto = new CreatePaymentRequestDto(rental.getId(), PaymentType.PAYMENT);

        Session stripeSession = mock(Session.class);
        when(stripeSession.getId()).thenReturn("sess_test_123");
        when(stripeSession.getUrl()).thenReturn("https://stripe.com/pay/test");

        when(stripeProvider.createSession(any(BigDecimal.class), anyString()))
                .thenReturn(stripeSession);

        // WHEN & THEN
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .with(user(user))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value("sess_test_123"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when user tries to pay for someone else's rental")
    void createPayment_UnauthorizedUser_ShouldReturnForbidden() throws Exception {
        // GIVEN
        User owner = createUser("owner@test.com");
        User otherUser = createUser("other@test.com");
        Car car = createCar();
        Rental rental = createRental(owner.getId(), car.getId());

        CreatePaymentRequestDto requestDto = new CreatePaymentRequestDto(rental.getId(), PaymentType.PAYMENT);

        Session stripeSession = mock(Session.class);
        when(stripeProvider.createSession(any(), anyString())).thenReturn(stripeSession);

        // WHEN & THEN
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .with(user(otherUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("password");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRoles(Set.of(customerRole));
        return userRepository.save(user);
    }

    private Car createCar() {
        Car car = new Car();
        car.setModel("Model S");
        car.setBrand("Tesla");
        CarType type = new CarType();
        type.setTypeName(TypeName.SEDAN);
        car.setType(type);
        car.setInventory(1);
        car.setDailyFee(BigDecimal.valueOf(100));
        return carRepository.save(car);
    }

    private Rental createRental(Long userId, Long carId) {
        Rental rental = new Rental();
        rental.setUserId(userId);
        rental.setCarId(carId);
        rental.setRentalDate(LocalDate.now());
        rental.setReturnDate(LocalDate.now().plusDays(2));
        return rentalRepository.save(rental);
    }
}
