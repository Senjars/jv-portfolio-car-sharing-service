package com.github.senjars.carsharing.notify;

import com.github.senjars.carsharing.model.rental.Rental;
import com.github.senjars.carsharing.repository.RentalRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RentalNotificationScheduler {

    private final RentalRepository rentalRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedRateString = "PT24H")
    public void checkOverdueRentals() {
        List<Rental> overdueRentals = rentalRepository
                .findAllByReturnDateBeforeAndActualReturnDateIsNull(LocalDate.now());

        if (overdueRentals.isEmpty()) {
            notificationService.sendMessage("✅ **No rentals overdue today!**\n"
                    + "All cars are back in the fleet.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("⚠️ **Overdue Rentals Report**\n\n");

        for (Rental rental : overdueRentals) {
            builder.append(String.format(
                    "🆔 **Rental ID:** %d\n"
                            + "🏎️ **Car ID:** %d\n"
                            + "👤 **User ID:** %d\n"
                            + "📅 **Due Date:** %s\n"
                            + "---------------------------\n",
                    rental.getId(),
                    rental.getCarId(),
                    rental.getUserId(),
                    rental.getReturnDate()
            ));
        }

        notificationService.sendMessage(builder.toString());
    }
}
