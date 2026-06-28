package com.github.senjars.carsharing.mapper;

import com.github.senjars.carsharing.dto.car.CarDto;
import com.github.senjars.carsharing.dto.car.CarShortDto;
import com.github.senjars.carsharing.dto.car.CreateCarDto;
import com.github.senjars.carsharing.dto.car.UpdateCarDto;
import com.github.senjars.carsharing.model.car.Car;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CarMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Car toEntity(CreateCarDto createCarDto);

    @Mapping(target = "type", source = "type.typeName")
    CarDto toDto(Car car);

    @Mapping(target = "type", source = "type.typeName")
    CarShortDto toShortDto(Car car);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "type", ignore = true)
    void updateCarDto(UpdateCarDto updateCarDto, @MappingTarget Car car);

}
