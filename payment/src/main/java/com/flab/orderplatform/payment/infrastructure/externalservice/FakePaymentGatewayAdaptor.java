package com.flab.orderplatform.payment.infrastructure.externalservice;

import com.flab.orderplatform.payment.application.port.out.PaymentGateway;
import com.flab.orderplatform.payment.application.port.out.PgApprovalRequest;
import com.flab.orderplatform.payment.application.port.out.PgApprovalResult;
import org.springframework.stereotype.Component;

@Component
public class FakePaymentGatewayAdaptor implements PaymentGateway {
    @Override
    public PgApprovalResult approve(PgApprovalRequest request) {
        long tail = request.amount() % 10;

        if (tail == 1) {
            return PgApprovalResult.fail();
        }
        return PgApprovalResult.success();
    }
}
