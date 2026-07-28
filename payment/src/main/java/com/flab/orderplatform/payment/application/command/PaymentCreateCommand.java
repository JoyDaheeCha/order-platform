package com.flab.orderplatform.payment.application.command;

import com.flab.orderplatform.payment.domain.Payment;
import lombok.Builder;

@Builder
public class PaymentCreateCommand {
    String orderNumber;
    Long buyerId;
    Long amount;

    public Payment create() {
        return Payment.create(orderNumber, buyerId, amount);
    }
}
