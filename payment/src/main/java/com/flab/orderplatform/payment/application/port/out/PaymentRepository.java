package com.flab.orderplatform.payment.application.port.out;

import com.flab.orderplatform.payment.domain.Payment;
import com.flab.orderplatform.payment.domain.status.PaymentStatus;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findByOrderNumberAndStatus(String orderNumber, PaymentStatus paymentStatus);

    Optional<Payment> findByOrderNumberAndStatusIn(String orderNumber, List<PaymentStatus> paymentStatuses);
}
