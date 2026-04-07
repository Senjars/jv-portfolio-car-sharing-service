package com.github.senjars.carsharing.mapper;

import com.github.senjars.carsharing.dto.rental.CreateRentalRequestDto;
import com.github.senjars.carsharing.dto.rental.RentalDto;
import com.github.senjars.carsharing.model.rental.Rental;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RentalMapper {

    RentalDto toDto(Rental rental);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "actualReturnDate", ignore = true)
    Rental toEntity(CreateRentalRequestDto createRentalRequestDto);

}
