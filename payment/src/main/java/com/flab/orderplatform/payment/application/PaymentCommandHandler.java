package com.flab.orderplatform.payment.application;

import com.flab.orderplatform.payment.application.annotation.PaymentTransactional;
import com.flab.orderplatform.payment.application.command.PaymentCompleteCommand;
import com.flab.orderplatform.payment.application.command.PaymentCreateCommand;
import com.flab.orderplatform.payment.application.exception.PaymentNotFoundException;
import com.flab.orderplatform.payment.application.port.out.PaymentRepository;
import com.flab.orderplatform.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.flab.orderplatform.payment.domain.status.PaymentStatus.FAILED;
import static com.flab.orderplatform.payment.domain.status.PaymentStatus.REQUESTED;

@Service
@RequiredArgsConstructor
public class PaymentCommandHandler {
    private final PaymentRepository paymentRepository;

    /**
     * 신규 결제 정보를 생성한다.
     * - 실패한 경우는 재시도 대상이므로 포함된다.
     */
    @PaymentTransactional
    public Payment handle(PaymentCreateCommand command) {
        var orderNumber = command.orderNumber();
        var searchedPayment = paymentRepository.findByOrderNumberAndStatus(orderNumber, FAILED);

        Payment payment;
        if (searchedPayment.isEmpty()) {
            payment = command.create();
        } else {
            payment = command.retry(searchedPayment.get());
        }

        return paymentRepository.save(payment);
    }

    @PaymentTransactional
    public Payment handle(PaymentCompleteCommand command) {
        var orderNumber = command.orderNumber();
        var requestedPayment = paymentRepository.findByOrderNumberAndStatus(orderNumber, REQUESTED)
                .orElseThrow(() -> new PaymentNotFoundException(orderNumber));
        var payment = command.complete(requestedPayment);
        return paymentRepository.save(payment);
    }
}
