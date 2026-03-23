package com.github.senjars.carsharing.service;

import com.github.senjars.carsharing.dto.car.CarDto;
import com.github.senjars.carsharing.dto.car.CreateCarDto;
import com.github.senjars.carsharing.dto.car.UpdateCarDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CarService {

    CarDto addCar(CreateCarDto createCarDto);

    void removeCar(Long carId);

    CarDto updateCar(Long carId, UpdateCarDto updateCarDto);

    CarDto getCarInfo(Long carId);

    Page<CarDto> getCars(Pageable pageable);
}
