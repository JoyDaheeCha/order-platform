package com.flab.orderplatform.payment.application.port.out;

import com.flab.orderplatform.payment.domain.Payment;
import com.flab.orderplatform.payment.domain.status.PaymentStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findByOrderNumberAndStatus(String orderNumber, PaymentStatus paymentStatus);

    Optional<Payment> findByOrderNumberAndStatusIn(String orderNumber, List<PaymentStatus> paymentStatuses);

    List<Payment> findByStatusAndPgRequestedAtBefore(PaymentStatus status, LocalDateTime threshold, Pageable pageable);
}
