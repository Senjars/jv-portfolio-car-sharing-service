package com.github.senjars.carsharing.repository;

import com.github.senjars.carsharing.model.payment.Payment;
import com.github.senjars.carsharing.model.payment.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p, Rental r "
            + "WHERE p.rentalId = r.id "
            + "AND r.userId = :userId")
    Page<Payment> findPaymentsByUserId(@Param("userId") Long userId, Pageable pageable);

    Optional<Payment> findPaymentBySessionId(String sessionId);

    List<Payment> findAllByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime time);

    @Query("SELECT COUNT(p) > 0 FROM Payment p, Rental r "
            + "WHERE p.rentalId = r.id "
            + "AND r.userId = :userId "
            + "AND p.status = :status")
    boolean existsByUserIdAndStatus(@Param("userId") Long userId,
                                    @Param("status") PaymentStatus status);

    Optional<Payment> findByRentalId(Long rentalId);
}
