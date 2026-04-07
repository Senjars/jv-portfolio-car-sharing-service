package com.github.senjars.carsharing.service.impl;

import com.github.senjars.carsharing.dto.rental.CreateRentalRequestDto;
import com.github.senjars.carsharing.dto.rental.RentalDto;
import com.github.senjars.carsharing.exception.AccessDeniedException;
import com.github.senjars.carsharing.exception.BadRequestException;
import com.github.senjars.carsharing.exception.EntityInventoryException;
import com.github.senjars.carsharing.exception.EntityNotFoundException;
import com.github.senjars.carsharing.exception.RentalAlreadyReturnedException;
import com.github.senjars.carsharing.mapper.RentalMapper;
import com.github.senjars.carsharing.model.car.Car;
import com.github.senjars.carsharing.model.rental.Rental;
import com.github.senjars.carsharing.repository.CarRepository;
import com.github.senjars.carsharing.repository.RentalRepository;
import com.github.senjars.carsharing.service.RentalService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {

    private final RentalRepository rentalRepository;
    private final RentalMapper rentalMapper;
    private final CarRepository carRepository;

    @Override
    @Transactional
    public RentalDto rentCar(Long userId, CreateRentalRequestDto requestDto) {
        if (requestDto.rentalDate().isAfter(requestDto.returnDate())) {
            throw new BadRequestException("Rental date must be before return date");
        }

        Car car = carRepository.findWithLockingById(requestDto.carId()).orElseThrow(
                () -> new EntityNotFoundException("Car with id "
                        + requestDto.carId() + " not found"));

        if (car.getInventory() < 1) {
            throw new EntityInventoryException("Car is out of stock");
        }

        car.setInventory(car.getInventory() - 1);
        carRepository.save(car);

        Rental rental = rentalMapper.toEntity(requestDto);
        rental.setUserId(userId);
        Rental savedRental = rentalRepository.save(rental);

        return rentalMapper.toDto(savedRental);
    }

    @Override
    @Transactional
    public RentalDto returnCar(Long userId, Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId).orElseThrow(
                () -> new EntityNotFoundException("Rental with id " + rentalId + " not found"));

        if (!rental.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to return this rental");
        }

        if (rental.getActualReturnDate() != null) {
            throw new RentalAlreadyReturnedException("This rental has already been returned!");
        }

        Car car = carRepository.findWithLockingById(rental.getCarId()).orElseThrow(
                () -> new EntityNotFoundException("Car with id "
                        + rental.getCarId() + " not found"));

        car.setInventory(car.getInventory() + 1);
        carRepository.save(car);

        rental.setActualReturnDate(LocalDate.now());
        Rental savedRental = rentalRepository.save(rental);

        return rentalMapper.toDto(savedRental);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RentalDto> getRentalsByUserIdAndStatus(Long userId, Boolean isActive,
                                                       Pageable pageable) {

        Specification<Rental> spec = Specification.where(null);

        if (userId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("userId"), userId));
        }

        if (isActive != null) {
            spec = spec.and(((root, query, cb) ->
                    isActive ? cb.isNull(root.get("actualReturnDate"))
                            : cb.isNotNull(root.get("actualReturnDate"))));
        }

        return rentalRepository.findAll(spec, pageable).map(rentalMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalDto getRentalById(Long rentalId, Long currentUserId, boolean isManager) {
        Rental rental = rentalRepository.findById(rentalId).orElseThrow(
                () -> new EntityNotFoundException("Rental with id " + rentalId + " not found"));

        if (!isManager && !rental.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You are not authorized to view this rental");
        }

        return rentalMapper.toDto(rental);
    }
}
