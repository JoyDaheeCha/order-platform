package com.flab.orderplatform.payment.application;

import com.flab.orderplatform.payment.application.command.PaymentCompleteCommand;
import com.flab.orderplatform.payment.application.command.PaymentCreateCommand;
import com.flab.orderplatform.payment.common.exception.PaymentNotFoundException;
import com.flab.orderplatform.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.flab.orderplatform.payment.domain.status.PaymentStatus.REQUESTED;

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

    @Transactional
    public Payment handle(PaymentCompleteCommand command) {
        var orderNumber = command.orderNumber();
        var requestedPayment = paymentRepository.findByOrderNumberAndStatus(orderNumber, REQUESTED)
                .orElseThrow(() -> new PaymentNotFoundException(orderNumber));
        var payment = command.complete(requestedPayment);
        return paymentRepository.save(payment);
    }
}
