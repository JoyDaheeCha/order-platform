package com.flab.orderplatform.payment.application;

import com.flab.orderplatform.payment.application.annotation.PaymentTransactional;
import com.flab.orderplatform.payment.application.command.PaymentCreateCommand;
import com.flab.orderplatform.payment.application.command.PaymentFinishCommand;
import com.flab.orderplatform.payment.application.command.PaymentStartCommand;
import com.flab.orderplatform.payment.application.exception.PaymentNotFoundException;
import com.flab.orderplatform.payment.application.port.out.PaymentRepository;
import com.flab.orderplatform.payment.domain.Payment;
import com.flab.orderplatform.payment.domain.status.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.flab.orderplatform.payment.domain.status.PaymentStatus.IN_PROGRESS;
import static com.flab.orderplatform.payment.domain.status.PaymentStatus.REQUESTED;

@Service
@RequiredArgsConstructor
public class PaymentCommandHandler {
    /**
     * PG 승인 요청 가능 상태값
     */
    private static final List<PaymentStatus> PG_APPROVAL_TARGET_STATUSES = List.of(PaymentStatus.FAILED, PaymentStatus.REQUESTED);

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 신규 결제 정보를 생성한다.
     * - 실패한 경우는 재시도 대상이므로 포함된다.
     */
    @PaymentTransactional
    public Payment handle(PaymentCreateCommand command) {
        var orderNumber = command.orderNumber();
        var searchedPayment = paymentRepository.findByOrderNumberAndStatusIn(orderNumber, PG_APPROVAL_TARGET_STATUSES);

        // 등록된 결제 이력이 없다면 신규 생성
        if (searchedPayment.isEmpty()) {
            var createdPayment = command.create();
            return paymentRepository.save(createdPayment);
        }

        // 실패한 전적이 있다면 재시도
        var existingPayment = searchedPayment.get();
        if (existingPayment.isFailed()) {
            var retryTarget = command.retry(existingPayment);
            return paymentRepository.save(retryTarget);
        }

        // 이미 생성되었다면 그대로 사용
        return existingPayment;
    }

    @PaymentTransactional
    public Payment handle(PaymentStartCommand command) {
        var orderNumber = command.orderNumber();
        var payment = paymentRepository.findByOrderNumberAndStatus(orderNumber, REQUESTED)
                .orElseThrow(() -> new PaymentNotFoundException(orderNumber));
        var paymentInProcess = command.start(payment);
        return paymentRepository.save(paymentInProcess);
    }

    @PaymentTransactional
    public Payment handle(PaymentFinishCommand command) {
        var orderNumber = command.orderNumber();
        var requestedPayment = paymentRepository.findByOrderNumberAndStatus(orderNumber, IN_PROGRESS)
                .orElseThrow(() -> new PaymentNotFoundException(orderNumber));
        var finishedPayment = command.finish(requestedPayment);

        paymentRepository.save(finishedPayment);
        // 결제 성공 상태일 경우 결제 완료 이벤트 발행
        if (finishedPayment.isCompleted()) {
            finishedPayment
                    .pullDomainEventIfPresent()
                    .ifPresent(eventPublisher::publishEvent);
        }
        return finishedPayment;
    }
}
