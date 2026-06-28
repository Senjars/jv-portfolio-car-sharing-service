package com.github.senjars.carsharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.senjars.carsharing.dto.car.CarDto;
import com.github.senjars.carsharing.dto.car.CarShortDto;
import com.github.senjars.carsharing.dto.rental.CreateRentalRequestDto;
import com.github.senjars.carsharing.dto.rental.RentalDetailsDto;
import com.github.senjars.carsharing.dto.rental.RentalDto;
import com.github.senjars.carsharing.exception.AccessDeniedException;
import com.github.senjars.carsharing.exception.BadRequestException;
import com.github.senjars.carsharing.exception.EntityInventoryException;
import com.github.senjars.carsharing.exception.EntityNotFoundException;
import com.github.senjars.carsharing.exception.PaymentException;
import com.github.senjars.carsharing.exception.RentalAlreadyReturnedException;
import com.github.senjars.carsharing.mapper.CarMapper;
import com.github.senjars.carsharing.mapper.RentalMapper;
import com.github.senjars.carsharing.model.car.Car;
import com.github.senjars.carsharing.model.car.CarType;
import com.github.senjars.carsharing.model.car.TypeName;
import com.github.senjars.carsharing.model.payment.PaymentStatus;
import com.github.senjars.carsharing.model.rental.Rental;
import com.github.senjars.carsharing.model.user.User;
import com.github.senjars.carsharing.notify.NotificationService;
import com.github.senjars.carsharing.repository.CarRepository;
import com.github.senjars.carsharing.repository.PaymentRepository;
import com.github.senjars.carsharing.repository.RentalRepository;
import com.github.senjars.carsharing.service.impl.RentalServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
public class RentalServiceImplTest {

    @InjectMocks
    private RentalServiceImpl rentalService;

    @Mock
    private CarRepository carRepository;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private RentalMapper rentalMapper;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CarMapper carMapper;

    @Test
    @DisplayName("Should throw PaymentException when user has pending payments")
    void rentCar_pendingPayments_throwsPaymentException() {
        // GIVEN
        Long userId = 1L;
        CreateRentalRequestDto request = new CreateRentalRequestDto(
                1L, LocalDate.now(), LocalDate.now().plusDays(1));

        when(paymentRepository.existsByUserIdAndStatus(userId, PaymentStatus.PENDING))
                .thenReturn(true);

        // WHEN & THEN
        assertThrows(PaymentException.class, () -> rentalService.rentCar(userId, request));
    }

    @Test
    @DisplayName("Should throw BadRequestException when rental date is after return date")
    void rentCar_invalidDates_throwsBadRequestException() {
        // GIVEN
        Long userId = 1L;
        CreateRentalRequestDto request = new CreateRentalRequestDto(
                1L, LocalDate.now().plusDays(5), LocalDate.now().plusDays(1));

        when(paymentRepository.existsByUserIdAndStatus(userId, PaymentStatus.PENDING))
                .thenReturn(false);

        // WHEN & THEN
        assertThrows(BadRequestException.class, () -> rentalService.rentCar(userId, request));
    }

    @Test
    @DisplayName("Should throw EntityInventoryException when car inventory is 0")
    void rentCar_outOfStock_throwsEntityInventoryException() {
        // GIVEN
        Long userId = 1L;
        CreateRentalRequestDto request = new CreateRentalRequestDto(
                1L, LocalDate.now(), LocalDate.now().plusDays(1));
        Car car = new Car();
        car.setInventory(0); // Brak aut

        when(paymentRepository.existsByUserIdAndStatus(userId, PaymentStatus.PENDING)).thenReturn(false);
        when(carRepository.findWithLockingById(1L)).thenReturn(Optional.of(car));

        // WHEN & THEN
        assertThrows(EntityInventoryException.class, () -> rentalService.rentCar(userId, request));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when user tries to return someone else's rental")
    void returnCar_wrongUser_throwsAccessDeniedException() {
        // GIVEN
        Long rentalId = 10L;
        Long ownerId = 1L;
        Long hackerId = 2L;
        Rental rental = new Rental();
        rental.setUserId(ownerId);

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

        // WHEN & THEN
        assertThrows(AccessDeniedException.class, () -> rentalService.returnCar(hackerId, rentalId));
    }

    @Test
    @DisplayName("Should throw RentalAlreadyReturnedException when car is already returned")
    void returnCar_alreadyReturned_throwsException() {
        // GIVEN
        Long userId = 1L;
        Long rentalId = 10L;
        Rental rental = new Rental();
        rental.setUserId(userId);
        rental.setActualReturnDate(LocalDate.now());

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

        // WHEN & THEN
        assertThrows(
                RentalAlreadyReturnedException.class, () -> rentalService.returnCar(userId, rentalId));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when regular user tries to view other user's rental")
    void getRentalById_unauthorizedUser_throwsAccessDeniedException() {
        // GIVEN
        Long rentalId = 10L;
        Long ownerId = 1L;
        Long otherUserId = 2L;
        Rental rental = new Rental();
        rental.setUserId(ownerId);

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

        // WHEN & THEN
        // isManager = false
        assertThrows(AccessDeniedException.class,
                () -> rentalService.getRentalById(rentalId, otherUserId, false));
    }

    @Test
    @DisplayName("Should allow manager to view any rental")
    void getRentalById_managerViewsAnyRental_returnsDto() {
        // GIVEN
        Long rentalId = 10L;
        Long ownerId = 1L;
        Long managerId = 99L;
        Rental rental = new Rental();
        rental.setId(rentalId);
        rental.setUserId(ownerId);
        rental.setCarId(1L);
        rental.setRentalDate(LocalDate.now());
        rental.setReturnDate(LocalDate.now());
        Car car = createCar();
        RentalDetailsDto expected = new RentalDetailsDto(
                rentalId, createCarShortDto(), ownerId,
                rental.getRentalDate(), rental.getReturnDate(), null);

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
        when(carRepository.findById(rental.getCarId())).thenReturn(Optional.of(car));
        when(rentalMapper.toDetailsDto(rental, car)).thenReturn(expected);

        // WHEN
        RentalDetailsDto actual = rentalService.getRentalById(rentalId, managerId, true);

        // THEN
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should successfully process a car rental when user has no pending payments and car is available")
    void rentCar_validRequest_returnsRentalDto() {
        // GIVEN
        Car car = createCar();
        User user = createUser();
        CreateRentalRequestDto requestDto = new CreateRentalRequestDto(
                1L, LocalDate.now(), LocalDate.now().plusDays(3));
        Rental rental = createRental();
        RentalDto expected = createRentalDto();

        when(rentalMapper.toEntity(requestDto)).thenReturn(rental);
        when(paymentRepository.existsByUserIdAndStatus(user.getId(), PaymentStatus.PENDING)).thenReturn(false);
        when(carRepository.findWithLockingById(requestDto.carId())).thenReturn(Optional.of(car));
        when(rentalRepository.save(any(Rental.class))).thenReturn(rental);
        when(rentalMapper.toDto(rental)).thenReturn(expected);

        // WHEN
        RentalDto actual = rentalService.rentCar(user.getId(), requestDto);

        // THEN
        assertThat(actual).isEqualTo(expected);
        verify(rentalRepository).save(any(Rental.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when attempting to rent a car that does not exist in the catalog")
    void rentCar_invalidCarId_throwsEntityNotFoundException() {
        // GIVEN
        Long invalidCarId = 999L;
        CreateRentalRequestDto rentalRequestDto = new CreateRentalRequestDto(
                invalidCarId, LocalDate.now(), LocalDate.now().plusDays(3));

        when(paymentRepository.existsByUserIdAndStatus(1L, PaymentStatus.PENDING)).thenReturn(false);
        when(carRepository.findWithLockingById(invalidCarId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> rentalService.rentCar(1L, rentalRequestDto));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when trying to process a return for a non-existent rental record")
    void returnCar_invalidId_throwsEntityNotFoundException() {
        // GIVEN
        Long invalidId = 999L;
        Long userId = 1L;

        when(rentalRepository.findById(invalidId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> rentalService.returnCar(userId, invalidId));
    }

    @Test
    @DisplayName("Should successfully finalize a rental by setting return date and restoring car inventory")
    void returnCar_validId_returnsRentalDto() {
        // GIVEN
        Long rentalId = 1L;
        Long userId = 1L;

        Car car = createCar();
        Rental rental = createRental();
        rental.setActualReturnDate(null);
        RentalDto expected = createRentalDto();

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
        when(carRepository.findWithLockingById(rental.getCarId())).thenReturn(Optional.of(car));
        when(rentalRepository.save(any(Rental.class))).thenReturn(rental);
        when(rentalMapper.toDto(rental)).thenReturn(expected);

        // WHEN
        RentalDto actual = rentalService.returnCar(userId, rentalId);

        // THEN
        assertThat(actual).isEqualTo(expected);
        verify(rentalRepository).findById(rentalId);
        verify(carRepository).findWithLockingById(rental.getCarId());
        verify(rentalRepository).save(any(Rental.class));
        verify(rentalMapper).toDto(rental);
    }

    @Test
    @DisplayName("Should return a filtered list of rentals based on user ID and their active/inactive status")
    void getRentalByUserIdAndStatus_validId_returnsRentalDto() {
        // GIVEN
        Long userId = 1L;
        Rental rental = createRental();
        RentalDto expected = createRentalDto();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Rental> rentalPage = new PageImpl<>(List.of(rental), pageable, 1);

        when(rentalRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(rentalPage);
        when(rentalMapper.toDto(rental)).thenReturn(expected);

        // WHEN
        List<RentalDto> actual = rentalService.getRentalsByUserIdAndStatus(userId, true, pageable).getContent();

        // THEN
        assertThat(actual).containsExactly(expected);
        assertThat(actual.get(0)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when no rentals match the specified user search criteria")
    void getRentalByUserIdAndStatus_invalidId_throwsEntityNotFoundException() {
        // GIVEN
        Long invalidId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        when(rentalRepository.findAll(any(Specification.class), eq(pageable))).thenThrow(
                new EntityNotFoundException("No rentals found for user id " + invalidId));

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class,
                () -> rentalService.getRentalsByUserIdAndStatus(invalidId, true, pageable));
    }

    @Test
    @DisplayName("Should retrieve detailed rental information when accessed by an authorized user or manager")
    void getRentalById_validId_returnsRentalDto() {
        // GIVEN
        Long rentalId = 1L;
        Long currentUserId = 1L;
        boolean isManager = true;
        Rental rental = createRental();
        Car car = createCar();
        RentalDetailsDto expected = createRentalDetailsDto();

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
        when(carRepository.findById(rental.getCarId())).thenReturn(Optional.of(car));
        when(rentalMapper.toDetailsDto(rental, car)).thenReturn(expected);

        // WHEN
        RentalDetailsDto actual = rentalService.getRentalById(rentalId, currentUserId, isManager);

        // THEN
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when searching for a rental ID that is not in the system")
    void getRentalById_invalidId_throwsEntityNotFoundException() {
        // GIVEN
       Long invalidId = 999L;
       Long currentUserId = 1L;
       boolean isManager = true;

       when(rentalRepository.findById(invalidId)).thenReturn(Optional.empty());

       // WHEN & THEN
       assertThrows(EntityNotFoundException.class,
               () -> rentalService.getRentalById(invalidId, currentUserId, isManager));
    }

    private Car createCar() {
        Car car = new Car();
        car.setId(1L);
        CarType type = new CarType();
        type.setTypeName(TypeName.SEDAN);
        car.setType(type);
        car.setModel("Test Model");
        car.setBrand("Test Brand");
        car.setInventory(1);
        car.setDailyFee(BigDecimal.valueOf(100));
        return car;
    }

    private RentalDto createRentalDto() {
        return new RentalDto(
                1L, 1L, 1L,
                LocalDate.now(), LocalDate.now().plusDays(3),
                null
        );
    }

    private RentalDetailsDto createRentalDetailsDto() {
        return new RentalDetailsDto(
                1L,
                createCarShortDto(),
                1L,
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                null
        );
    }

    private CarShortDto createCarShortDto() {
        return new CarShortDto(
                1L,
                TypeName.SEDAN.name(),
                "Test Brand",
                "Test Model",
                BigDecimal.valueOf(100)
        );
    }

    private CarDto createCarDto() {
        return new CarDto(1L, TypeName.SEDAN, "Test Brand", "Test Model", 1,
                BigDecimal.valueOf(100));
    }

    private Rental createRental() {
        Rental rental = new Rental();
        rental.setId(1L);
        rental.setUserId(1L);
        rental.setCarId(1L);
        rental.setRentalDate(LocalDate.now());
        rental.setReturnDate(LocalDate.now().plusDays(3));
        return rental;
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        return user;
    }
}
