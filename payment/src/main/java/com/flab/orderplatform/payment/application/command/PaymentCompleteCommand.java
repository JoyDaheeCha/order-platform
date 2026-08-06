package com.flab.orderplatform.payment.application.command;

import com.flab.orderplatform.payment.domain.Payment;
import lombok.Builder;

@Builder
public record PaymentCompleteCommand(
        String orderNumber,
        boolean isPaymentSucceed,
        String failureReason,
        String pgTid
) {
    public Payment complete(Payment payment) {
        return payment.complete(isPaymentSucceed, failureReason, pgTid);
    }
}
