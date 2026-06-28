package com.github.senjars.carsharing.service.impl;

import com.github.senjars.carsharing.dto.car.CarDto;
import com.github.senjars.carsharing.dto.car.CreateCarDto;
import com.github.senjars.carsharing.dto.car.UpdateCarDto;
import com.github.senjars.carsharing.exception.EntityNotFoundException;
import com.github.senjars.carsharing.mapper.CarMapper;
import com.github.senjars.carsharing.model.car.Car;
import com.github.senjars.carsharing.model.car.CarType;
import com.github.senjars.carsharing.repository.CarRepository;
import com.github.senjars.carsharing.repository.CarTypeRepository;
import com.github.senjars.carsharing.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {

    private final CarRepository carRepository;
    private final CarMapper carMapper;
    private final CarTypeRepository carTypeRepository;

    @Override
    @Transactional
    public CarDto addCar(CreateCarDto createCarDto) {
        Car car = carMapper.toEntity(createCarDto);
        CarType carType = carTypeRepository.findByTypeName(createCarDto.type())
                .orElseThrow(() -> new EntityNotFoundException("Car type "
                        + createCarDto.type() + " not found"));

        car.setType(carType);
        Car savedCar = carRepository.save(car);
        return carMapper.toDto(savedCar);
    }

    @Override
    @Transactional
    public void removeCar(Long carId) {
        carRepository.findById(carId).orElseThrow(
                () -> new EntityNotFoundException("Car with id " + carId + " not found"));

        carRepository.deleteById(carId);
    }

    @Override
    @Transactional
    public CarDto updateCar(Long carId, UpdateCarDto updateCarDto) {
        Car car = carRepository.findById(carId).orElseThrow(
                () -> new EntityNotFoundException("Car with id " + carId + " not found"));

        carMapper.updateCarDto(updateCarDto, car);

        if (updateCarDto.type() != null) {
            CarType carType = carTypeRepository.findByTypeName(updateCarDto.type())
                    .orElseThrow(() -> new EntityNotFoundException("Car type "
                            + updateCarDto.type() + " not found"));
            car.setType(carType);
        }

        Car savedCar = carRepository.save(car);
        return carMapper.toDto(savedCar);
    }

    @Override
    @Transactional(readOnly = true)
    public CarDto getCarInfo(Long carId) {
        Car car = carRepository.findById(carId).orElseThrow(
                () -> new EntityNotFoundException("Car with id " + carId + " not found"));

        return carMapper.toDto(car);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarDto> getCars(Pageable pageable) {
        return carRepository.findAll(pageable).map(carMapper::toDto);
    }
}
