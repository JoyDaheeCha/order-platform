package com.flab.orderplatform.payment.infrastructure.persistence;

import com.flab.orderplatform.payment.domain.Payment;
import com.flab.orderplatform.payment.domain.status.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentJapRepository extends JpaRepository<Long, Payment> {
    Payment save(Payment payment);

    Optional<Payment> findByOrderNumberAndStatus(String orderNumber, PaymentStatus status);
}
