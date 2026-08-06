package com.flab.orderplatform.payment.application;

import com.flab.orderplatform.payment.application.command.PaymentCompleteCommand;
import com.flab.orderplatform.payment.application.command.PaymentCreateCommand;
import com.flab.orderplatform.payment.application.port.out.PaymentGateway;
import com.flab.orderplatform.payment.application.port.out.PaymentRepository;
import com.flab.orderplatform.payment.application.port.out.PgApprovalRequest;
import com.flab.orderplatform.payment.domain.Payment;
import com.flab.orderplatform.payment.domain.status.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 결제 퍼사드
 */
@Component
@RequiredArgsConstructor
public class PaymentFacade {
    /**
     * 결제에서 제외되는 결제상태값
     */
    public static final List<PaymentStatus> PAYMENT_STATUSES_NOT_FOR_PAY = List.of(PaymentStatus.COMPLETED, PaymentStatus.REFUNDED);

    private final PaymentRepository paymentRepository;
    private final PaymentCommandHandler paymentCommandHandler;
    private final PaymentGateway paymentGateway;

    public Payment pay(PaymentCreateCommand createCommand) {
        var paymentRegisteredOrCompleted = paymentRepository.findByOrderNumberAndStatusIn(createCommand.orderNumber(), PAYMENT_STATUSES_NOT_FOR_PAY);

        // 이미 결제완료, 환불된 경우 PG 연동 하지 않는다.
        if (paymentRegisteredOrCompleted.isPresent()) {
            return paymentRegisteredOrCompleted.get();
        }

        // 결제 생성
        paymentCommandHandler.handle(createCommand);

        // PG 연동
        var pgApprovalRequest = PgApprovalRequest
                .builder()
                .orderNumber(createCommand.orderNumber())
                .amount(createCommand.amount())
                .build();
        var pgApprovalResult = paymentGateway.approve(pgApprovalRequest);
        var completeCommand = PaymentCompleteCommand
                .builder()
                .orderNumber(createCommand.orderNumber())
                .isPaymentSucceed(pgApprovalResult.isSucceed())
                .failureReason(pgApprovalResult.message())
                .pgTid(pgApprovalResult.tid())
                .build();

        // 결제 완료/실패 처리
        return paymentCommandHandler.handle(completeCommand);
    }
}
