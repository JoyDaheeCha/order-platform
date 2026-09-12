package com.flab.orderplatform.payment.application.command;

import com.flab.orderplatform.payment.domain.Payment;
import lombok.Builder;

@Builder
public record PaymentCompleteRefundCommand(
        String orderNumber,
        Boolean isRefundSucceed,
        String cancelFailureReason

) {
    public Payment completeRefund(Payment payment) {
        return payment.completeRefund(isRefundSucceed, cancelFailureReason);
    }
}
