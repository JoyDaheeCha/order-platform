package com.flab.orderplatform.payment.application;

import com.flab.orderplatform.payment.domain.Payment;
import com.flab.orderplatform.payment.domain.status.PaymentStatus;

import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findByOrderNumberAndStatus(String orderNumber, PaymentStatus paymentStatus);
}
