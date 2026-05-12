package com.github.senjars.carsharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.senjars.carsharing.dto.car.CarDto;
import com.github.senjars.carsharing.dto.car.CreateCarDto;
import com.github.senjars.carsharing.dto.car.UpdateCarDto;
import com.github.senjars.carsharing.exception.EntityNotFoundException;
import com.github.senjars.carsharing.mapper.CarMapper;
import com.github.senjars.carsharing.model.car.Car;
import com.github.senjars.carsharing.model.car.CarType;
import com.github.senjars.carsharing.model.car.TypeName;
import com.github.senjars.carsharing.repository.CarRepository;
import com.github.senjars.carsharing.repository.CarTypeRepository;
import com.github.senjars.carsharing.service.impl.CarServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CarServiceImplTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private CarMapper carMapper;

    @Mock
    private CarTypeRepository carTypeRepository;

    @InjectMocks
    private CarServiceImpl carService;

    @Test
    @DisplayName("Should successfully add a new car when valid DTO and car type are provided")
    void addCar_validDto_returnsCarDto() {
        // GIVEN
        CreateCarDto createCarDto = createNewCreateCarDto();
        Car car = new Car();
        CarType carType = new CarType();
        Car savedCar = new Car();
        CarDto expectedDto = new CarDto(1L, TypeName.SEDAN, "Model", "Brand", 1, BigDecimal.valueOf(100));

        // WHEN
        when(carMapper.toEntity(createCarDto)).thenReturn(car);
        when(carTypeRepository.findByTypeName(TypeName.SEDAN)).thenReturn(Optional.of(carType));
        when(carRepository.save(car)).thenReturn(savedCar);
        when(carMapper.toDto(savedCar)).thenReturn(expectedDto);

        CarDto result = carService.addCar(createCarDto);

        // THEN
        assertThat(result).isEqualTo(expectedDto);
        verify(carRepository).save(car);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when attempting to add a car with a non-existent type")
    void addCar_carTypeNotInDatabase_throwsException() {
        // GIVEN
        CreateCarDto createCarDto = createNewCreateCarDto();

        // WHEN
        when(carMapper.toEntity(createCarDto)).thenReturn(new Car());
        when(carTypeRepository.findByTypeName(TypeName.SEDAN)).thenReturn(Optional.empty());

        // THEN
        assertThatThrownBy(() -> carService.addCar(createCarDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Car type SEDAN not found");

        verify(carRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully remove a car from the fleet when a valid ID is provided")
    void removeCar_validId_deletesCar() {
        // GIVEN
        Long carId = 1L;
        Car car = new Car();

        // WHEN
        when(carRepository.findById(carId)).thenReturn(Optional.of(car));

        carService.removeCar(carId);

        // THEN
        verify(carRepository).deleteById(carId);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when trying to remove a car that does not exist")
    void removeCar_invalidId_throwsException() {
        // GIVEN
        Long invalidCarId = 999L;

        // WHEN
        when(carRepository.findById(invalidCarId)).thenReturn(Optional.empty());

        // THEN
        Assertions.assertThrows(EntityNotFoundException.class,
                () -> carService.removeCar(invalidCarId));

        verify(carRepository, never()).deleteById(invalidCarId);
    }

    @Test
    @DisplayName("Should successfully update car details and return the updated DTO")
    void updateCar_validId_returnsUpdatedCar() {
        // GIVEN
        Car car = createCar();
        UpdateCarDto updateCarDto = new UpdateCarDto(TypeName.SEDAN, "New Model", "New Brand", 2, BigDecimal.valueOf(200));
        CarDto expectedCarDto = createNewCarDto();

        // WHEN
        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));
        when(carMapper.toDto(car)).thenReturn(expectedCarDto);
        when(carRepository.save(car)).thenReturn(car);

        CarDto actualUpdatedCarDto = carService.updateCar(car.getId(), updateCarDto);

        // THEN
        Assertions.assertEquals(expectedCarDto, actualUpdatedCarDto);
        verify(carRepository).save(car);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when attempting to update a non-existent car")
    void updateCar_invalidId_throwsException() {
        // GIVEN
        Long invalidCarId = 999L;
        UpdateCarDto updateCarDto = new UpdateCarDto(TypeName.SEDAN, "New Model", "New Brand", 2, BigDecimal.valueOf(200));

        // WHEN
        when(carRepository.findById(invalidCarId)).thenReturn(Optional.empty());

        // THEN
        Assertions.assertThrows(EntityNotFoundException.class,
                () -> carService.updateCar(invalidCarId, updateCarDto));
    }

    @Test
    @DisplayName("Should successfully retrieve car information by its ID")
    void getCarInfo_validId_returnsCarDto() {
        // GIVEN
        Car car = createCar();
        CarDto expectedCarDto = createNewCarDto();

        // WHEN
        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));
        when(carMapper.toDto(car)).thenReturn(expectedCarDto);

        CarDto carDto = carService.getCarInfo(car.getId());

        // THEN
        assertThat(carDto).isEqualTo(expectedCarDto);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when requesting info for a non-existent car")
    void getCarInfo_invalidId_throwsException() {
        // GIVEN
        Long invalidCarId = 999L;

        // WHEN
        when(carRepository.findById(invalidCarId)).thenReturn(Optional.empty());

        // THEN
        Assertions.assertThrows(EntityNotFoundException.class,
                () -> carService.getCarInfo(invalidCarId));
    }

    private CreateCarDto createNewCreateCarDto() {
        return new CreateCarDto(
                TypeName.SEDAN,
                "Model",
                "Brand",
                1,
                BigDecimal.valueOf(100));
    }

    private CarDto createNewCarDto() {
        return new CarDto(
                1L,
                TypeName.SEDAN,
                "Model",
                "Brand",
                1,
                BigDecimal.valueOf(100));
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
