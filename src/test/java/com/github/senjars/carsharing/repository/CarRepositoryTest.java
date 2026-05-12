package com.github.senjars.carsharing.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.github.senjars.carsharing.model.car.Car;
import com.github.senjars.carsharing.model.car.CarType;
import com.github.senjars.carsharing.model.car.TypeName;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@DataJpaTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class CarRepositoryTest {

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CarTypeRepository carTypeRepository;

    @Test
    @DisplayName("Should successfully retrieve a car with a pessimistic lock by its ID")
    void findWithLockingById_validId_returnsCar() {
        // GIVEN
        Car car = createCar();
        Car savedCar = carRepository.save(car);
        Long generatedId = savedCar.getId();

        // WHEN
        Optional<Car> foundCar = carRepository.findWithLockingById(generatedId);

        // THEN
        assertThat(foundCar).isPresent();
        assertThat(foundCar.get().getId()).isEqualTo(generatedId);
    }

    @Test
    @DisplayName("Should return an empty optional when trying to lock a non-existent car")
    void findWithLockingById_invalidId_returnsEmpty() {
        // GIVEN
        Long nonExistingId = 999L;

        // WHEN
        Optional<Car> car = carRepository.findWithLockingById(nonExistingId);

        // THEN
        assertThat(car).isEmpty();
    }

    private Car createCar() {
        CarType type = new CarType();
        type.setTypeName(TypeName.SEDAN);
        Car car = new Car();
        car.setType(type);
        car.setModel("Test Model");
        car.setBrand("Test Brand");
        car.setInventory(1);
        car.setDailyFee(BigDecimal.valueOf(100));
        return car;
    }
}
