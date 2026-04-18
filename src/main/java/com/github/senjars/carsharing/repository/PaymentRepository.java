package com.github.senjars.carsharing.repository;

import com.github.senjars.carsharing.model.payment.Payment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p WHERE p.rentalId IN (SELECT r.id FROM "
            + "Rental r WHERE r.userId = :userId)")
    Page<Payment> findPaymentsByUserId(@Param("userId") Long userId, Pageable pageable);

    Optional<Payment> findPaymentBySessionId(String sessionId);

    boolean existsByRentalId(Long rentalId);
}
