package com.github.senjars.carsharing.mapper;

import com.github.senjars.carsharing.dto.rental.CreateRentalRequestDto;
import com.github.senjars.carsharing.dto.rental.RentalDetailsDto;
import com.github.senjars.carsharing.dto.rental.RentalDto;
import com.github.senjars.carsharing.model.car.Car;
import com.github.senjars.carsharing.model.rental.Rental;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CarMapper.class})
public interface RentalMapper {

    RentalDto toDto(Rental rental);

    @Mapping(target = "id", source = "rental.id")
    @Mapping(target = "car", source = "car")
    @Mapping(target = "userId", source = "rental.userId")
    @Mapping(target = "rentalDate", source = "rental.rentalDate")
    @Mapping(target = "returnDate", source = "rental.returnDate")
    @Mapping(target = "actualReturnDate", source = "rental.actualReturnDate")
    RentalDetailsDto toDetailsDto(Rental rental, Car car);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "actualReturnDate", ignore = true)
    @Mapping(target = "userId", ignore = true)
    Rental toEntity(CreateRentalRequestDto createRentalRequestDto);

}
