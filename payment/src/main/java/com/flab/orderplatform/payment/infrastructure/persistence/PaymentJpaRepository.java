package com.flab.orderplatform.payment.infrastructure.persistence;

import com.flab.orderplatform.payment.domain.Payment;
import com.flab.orderplatform.payment.domain.status.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderNumberAndStatus(String orderNumber, PaymentStatus status);

    Optional<Payment> findByOrderNumberAndStatusIn(String orderNumber, List<PaymentStatus> paymentStatuses);

    List<Payment> findByStatusAndPgRequestedAtBefore(PaymentStatus status, LocalDateTime threshold, Pageable pageable);
}
