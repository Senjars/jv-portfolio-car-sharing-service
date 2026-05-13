package com.github.senjars.carsharing.repository;

import com.github.senjars.carsharing.model.rental.Rental;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long>,
        JpaSpecificationExecutor<Rental> {

    List<Rental> findAllByReturnDateBeforeAndActualReturnDateIsNull(LocalDate returnDate);
}
