package com.flab.orderplatform.payment.application.command;

import com.flab.orderplatform.payment.domain.Payment;
import lombok.Builder;

@Builder
public record PaymentStartCommand(
        String orderNumber
) {
    public Payment start(Payment payment) {
        return payment.start();
    }
}
