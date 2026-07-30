package com.flab.orderplatform.payment.application;

import com.flab.orderplatform.payment.application.command.PaymentCompleteCommand;
import com.flab.orderplatform.payment.application.command.PaymentCreateCommand;
import com.flab.orderplatform.payment.application.port.out.PaymentRepository;
import com.flab.orderplatform.payment.common.exception.PaymentNotFoundException;
import com.flab.orderplatform.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.flab.orderplatform.payment.domain.status.PaymentStatus.FAILED;
import static com.flab.orderplatform.payment.domain.status.PaymentStatus.REQUESTED;

@Service
@RequiredArgsConstructor
public class PaymentCommandHandler {
    private final PaymentRepository paymentRepository;

    // TODO 별도 Tansaction으로 만들기
    /**
     * 신규 결제 정보를 생성한다.
     * - 이미 결제가 요청되었거나 실패한 경우도 결제 시도 대상에 포함한다.
     */
    @Transactional
    public Payment handle(PaymentCreateCommand command) {
        var orderNumber = command.orderNumber();
        var payment = paymentRepository.findByOrderNumberAndStatusIn(orderNumber, List.of(REQUESTED, FAILED))
                .orElse(command.create());
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
