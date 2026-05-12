package com.github.senjars.carsharing.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.github.senjars.carsharing.config.TestcontainersConfiguration;
import com.github.senjars.carsharing.model.car.Car;
import com.github.senjars.carsharing.model.car.CarType;
import com.github.senjars.carsharing.model.car.TypeName;
import com.github.senjars.carsharing.model.rental.Rental;
import com.github.senjars.carsharing.model.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@DataJpaTest
@Transactional
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RentalRepositoryTest {

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Test
    @DisplayName("Should find all overdue rentals where the car has not been returned yet")
    void findAllByReturnDateBeforeAndActualReturnDateIsNull_validInput_returnsRentals() {
        // GIVEN
        Car car = createCar();
        carRepository.save(car);

        User user = userRepository.save(createUser());

        Rental rental = createRental(user.getId(), car.getId());
        rentalRepository.save(rental);

        // WHEN
        List<Rental> result = rentalRepository.findAllByReturnDateBeforeAndActualReturnDateIsNull(
                LocalDate.now());

        // THEN
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.getFirst().getActualReturnDate()).isNull();
        assertThat(result.getFirst().getUserId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("Should return an empty list when no rentals are overdue relative to the provided date")
    void findAllByReturnDateBeforeAndActualReturnDateIsNull_invalidInput_returnsEmptyList() {
        // GIVEN
        Car car = carRepository.save(createCar());
        User user = userRepository.save(createUser());

        Rental rental = createRental(user.getId(), car.getId());
        rentalRepository.save(rental);

        // WHEN
        List<Rental> rentals = rentalRepository
                .findAllByReturnDateBeforeAndActualReturnDateIsNull(rental.getReturnDate());

        // THEN
        assertThat(rentals.isEmpty()).isTrue();
    }

    private User createUser() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("123Password123!");
        user.setFirstName("John");
        user.setLastName("Doe");
        return user;
    }

    private Rental createRental(Long userId, Long carId) {
        Rental rental = new Rental();
        rental.setUserId(userId);
        rental.setCarId(carId);
        rental.setRentalDate(LocalDate.now().minusDays(5));
        rental.setReturnDate(LocalDate.now().minusDays(2));
        return rental;
    }

    private Car createCar() {
        Car car = new Car();
        CarType type = new CarType();
        type.setTypeName(TypeName.SEDAN);
        car.setType(type);
        car.setModel("Test Model");
        car.setBrand("Test Brand");
        car.setInventory(1);
        car.setDailyFee(BigDecimal.valueOf(100));
        return car;
    }
}
