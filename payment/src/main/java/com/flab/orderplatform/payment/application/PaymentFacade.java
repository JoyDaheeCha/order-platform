package com.flab.orderplatform.payment.application;

import com.flab.orderplatform.payment.application.command.PaymentCompleteCommand;
import com.flab.orderplatform.payment.application.command.PaymentCreateCommand;
import com.flab.orderplatform.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 결제 퍼사드
 */
@Component
@RequiredArgsConstructor
public class PaymentFacade {
    private final PaymentCommandHandler paymentCommandHandler;

    public Payment pay(PaymentCreateCommand createCommand) {
        paymentCommandHandler.handle(createCommand);
        // TODO: PG 호출
        // TODO: 결제 완료 관련하여 어떤 정보 저장할지 찾아보자
        var isPaymentSucceed = true;
        var completeCommand = PaymentCompleteCommand
                .builder()
                .orderNumber(createCommand.orderNumber())
                .isPaymentSucceed(isPaymentSucceed)
                .build();
        return paymentCommandHandler.handle(completeCommand);
    }
}
