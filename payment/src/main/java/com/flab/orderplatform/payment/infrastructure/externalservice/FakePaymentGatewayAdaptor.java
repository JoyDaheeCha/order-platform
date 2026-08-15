package com.flab.orderplatform.payment.infrastructure.externalservice;

import com.flab.orderplatform.payment.application.port.out.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeoutException;

@Component
public class FakePaymentGatewayAdaptor implements PaymentGateway {
    @Override
    public PgApprovalResult approve(PgApprovalRequest request) {
        long tail = request.amount() % 10;

        if (tail == 1) {
            return PgApprovalResult.fail();
        }
        if (tail == 2) {
            throw new PaymentException(request.orderNumber(), new TimeoutException("PG 서비스 타임아웃으로 결제 승인요청을 실패 하였습니다."));
        }
        return PgApprovalResult.success();
    }

    @Override
    public PgSearchResult get(String orderNumber) {
        if (orderNumber.contains("paid")) {
            return new PgSearchResult("111-222-333-444", true); // 결제 성공 기록
        }
        return new PgSearchResult("111-222-333-444", false); // 결제 실패 기록
    }
}
