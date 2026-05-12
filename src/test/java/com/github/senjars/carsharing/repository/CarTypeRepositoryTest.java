package com.github.senjars.carsharing.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.github.senjars.carsharing.config.TestcontainersConfiguration;
import com.github.senjars.carsharing.model.car.CarType;
import com.github.senjars.carsharing.model.car.TypeName;
import java.util.Optional;
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
public class CarTypeRepositoryTest {

    @Autowired
    private CarTypeRepository carTypeRepository;

    @Test
    @DisplayName("Should successfully find a car type by its specific name enum")
    void findByTypeName_validTypeName_returnsCarType() {
        // GIVEN
        TypeName carTypeName = TypeName.SEDAN;
        CarType carType = new CarType();
        carType.setTypeName(carTypeName);
        carTypeRepository.save(carType);

        // WHEN
        Optional<CarType> foundCarType = carTypeRepository.findByTypeName(carTypeName);

        // THEN
        assertThat(foundCarType).isPresent();
        assertThat(foundCarType.get().getTypeName()).isEqualTo(carTypeName);
    }

    @Test
    @DisplayName("Should return an empty optional when searching for a null or non-existent car type")
    void findByTypeName_invalidTypeName_returnsEmpty() {
        // GIVEN
        TypeName carTypeName = null;

        // WHEN
        Optional<CarType> carType = carTypeRepository.findByTypeName(carTypeName);

        // THEN
        assertThat(carType).isEmpty();
    }
}
