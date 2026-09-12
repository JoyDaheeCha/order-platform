package com.flab.orderplatform.payment.application.command;

import com.flab.orderplatform.payment.domain.Payment;
import lombok.Builder;

@Builder
public record PaymentRequestRefundCommand(
        String orderNumber
) {
    public Payment requestRefund(Payment payment) {
        return payment.requestRefund();
    }
}
