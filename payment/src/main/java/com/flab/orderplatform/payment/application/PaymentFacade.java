package com.flab.orderplatform.payment.application;

import com.flab.orderplatform.payment.application.command.PaymentCreateCommand;
import com.flab.orderplatform.payment.application.command.PaymentFinishCommand;
import com.flab.orderplatform.payment.application.command.PaymentStartCommand;
import com.flab.orderplatform.payment.application.port.out.*;
import com.flab.orderplatform.payment.domain.Payment;
import com.flab.orderplatform.payment.domain.status.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.flab.orderplatform.payment.domain.status.PaymentStatus.*;

/**
 * 결제 퍼사드
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {
    /**
     * 결제에서 제외되는 결제상태값
     */
    public static final List<PaymentStatus> PAYMENT_STATUSES_NOT_FOR_PAY = List.of(IN_PROGRESS, COMPLETED, REFUNDED);

    private final PaymentRepository paymentRepository;
    private final PaymentCommandHandler paymentCommandHandler;
    private final PaymentGateway paymentGateway;

    public Payment pay(PaymentCreateCommand createCommand) {
        var orderNumber = createCommand.orderNumber();
        var pgExcludedPayment = paymentRepository.findByOrderNumberAndStatusIn(orderNumber, PAYMENT_STATUSES_NOT_FOR_PAY);

        // 이미 결제 처리중, 결제완료, 환불된 경우 PG 연동 하지 않는다.
        if (pgExcludedPayment.isPresent()) {
            return pgExcludedPayment.get();
        }

        // 결제 생성
        paymentCommandHandler.handle(createCommand);

        // 결제 처리 시작
        paymentCommandHandler.handle(new PaymentStartCommand(orderNumber));
        return integratePgService(createCommand);
    }

    private Payment integratePgService(PaymentCreateCommand createCommand) {
        var orderNumber = createCommand.orderNumber();
        // PG 연동
        PgApprovalResult pgApprovalResult;
        try {
            var pgApprovalRequest = PgApprovalRequest
                    .builder()
                    .orderNumber(orderNumber)
                    .amount(createCommand.amount())
                    .build();
            pgApprovalResult = paymentGateway.approve(pgApprovalRequest);
        } catch (PaymentException e) {
            log.error("주문번호 {} PG 연동 실패. 스케줄러로 원복 예정", orderNumber);
            throw e;
        }
        var completeCommand = PaymentFinishCommand
                .builder()
                .orderNumber(orderNumber)
                .isPaymentSucceed(pgApprovalResult.isSucceed())
                .failureReason(pgApprovalResult.message())
                .pgTid(pgApprovalResult.tid())
                .build();

        // 결제 완료/실패 처리
        return paymentCommandHandler.handle(completeCommand);
    }

    /**
     * PG 호출후 '결제 처리중' 상태로 계속 남아있는 데이터를 재처리한다.
     */
    public void retryPaymentInProcess() {
        var threshold = LocalDateTime.now().minusMinutes(5);
        var payments = paymentRepository.findByStatusAndPgRequestedAtBefore(IN_PROGRESS, threshold, PageRequest.of(0, 100));
        for (Payment payment : payments) {
            try {
                retry(payment);
            } catch (PaymentException e) {
                log.error("결제 재시도 실패. 주문번호: {}", payment.getOrderNumber(), e);
            }
        }
    }

    private void retry(Payment payment) {
        var orderNumber = payment.getOrderNumber();
        var pgPreviousResult = paymentGateway.get(orderNumber);
        // 결제가 이미 성공한 경우 -> 성공 처리
        if (pgPreviousResult.isPaid()) {
            var completeCommand = PaymentFinishCommand
                    .builder()
                    .orderNumber(orderNumber)
                    .isPaymentSucceed(true)
                    .failureReason(null)
                    .pgTid(pgPreviousResult.tid())
                    .build();
            // 결제 완료 처리
            paymentCommandHandler.handle(completeCommand);
            return;
        }
        // PG 사에서 결제 성공 기록이 없는 경우 -> 재처리
        var paymentCreateCommand = PaymentCreateCommand.builder()
                .orderNumber(orderNumber)
                .buyerId(payment.getBuyerId())
                .amount(payment.getAmount())
                .build();
        integratePgService(paymentCreateCommand);
    }
}
