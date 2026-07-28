package com.flab.orderplatform.payment.application;

import com.flab.orderplatform.payment.application.command.PaymentCreateCommand;
import com.flab.orderplatform.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentCommandHandler {
    PaymentRepository paymentRepository;

    // TODO 호출부에 PG 입히기
    // TODO 별도 Tansaction으로 만들기
    @Transactional
    public Payment handle(PaymentCreateCommand command) {
        var payment = command.create();
        return paymentRepository.save(payment);
    }
}
