package com.github.senjars.carsharing.notify;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.senjars.carsharing.model.rental.Rental;
import com.github.senjars.carsharing.repository.RentalRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RentalNotificationSchedulerTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private TelegramService telegramService;

    @InjectMocks
    private RentalNotificationScheduler notificationScheduler;

    @Test
    @DisplayName("Should send 'No overdue' message when no overdue rentals are found")
    void checkOverdueRentals_EmptyList_SendsSuccessMessage() {
        // GIVEN
        when(rentalRepository.findAllByReturnDateBeforeAndActualReturnDateIsNull(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // WHEN
        notificationScheduler.checkOverdueRentals();

        // THEN
        verify(telegramService).sendMessage(contains("No rentals overdue today"));
        verifyNoMoreInteractions(telegramService);
    }

    @Test
    @DisplayName("Should send detailed report when overdue rentals exist")
    void checkOverdueRentals_WithOverdueItems_SendsReport() {
        // GIVEN
        Rental overdue1 = createRental(1L, 101L, 201L, LocalDate.now().minusDays(1));
        Rental overdue2 = createRental(2L, 102L, 202L, LocalDate.now().minusDays(2));

        when(rentalRepository.findAllByReturnDateBeforeAndActualReturnDateIsNull(any(LocalDate.class)))
                .thenReturn(List.of(overdue1, overdue2));

        // WHEN
        notificationScheduler.checkOverdueRentals();

        // THEN
        verify(telegramService).sendMessage(argThat(msg ->
                msg.contains("Overdue Rentals Report") &&
                        msg.contains("**Rental ID:** 1") &&
                        msg.contains("**Rental ID:** 2") &&
                        msg.contains("**Car ID:** 101") &&
                        msg.contains("**User ID:** 202")
        ));
    }

    private Rental createRental(Long id, Long carId, Long userId, LocalDate returnDate) {
        Rental rental = new Rental();
        rental.setId(id);
        rental.setCarId(carId);
        rental.setUserId(userId);
        rental.setReturnDate(returnDate);
        return rental;
    }
}
