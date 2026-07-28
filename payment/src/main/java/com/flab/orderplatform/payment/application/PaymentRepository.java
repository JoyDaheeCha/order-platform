package com.flab.orderplatform.payment.application;

import com.flab.orderplatform.payment.domain.Payment;

public interface PaymentRepository {
    Payment save(Payment payment);
}
