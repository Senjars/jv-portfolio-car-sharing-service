package com.github.senjars.carsharing.service;

import com.github.senjars.carsharing.dto.rental.CreateRentalRequestDto;
import com.github.senjars.carsharing.dto.rental.RentalDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface RentalService {

    RentalDto rentCar(Long userId, CreateRentalRequestDto createRentalRequestDto);

    RentalDto returnCar(Long userId, Long rentalId);

    Page<RentalDto> getRentalsByUserIdAndStatus(Long userId, Boolean isActive, Pageable pageable);

    RentalDto getRentalById(Long rentalId, Long currentUserId, boolean isManager);
}
