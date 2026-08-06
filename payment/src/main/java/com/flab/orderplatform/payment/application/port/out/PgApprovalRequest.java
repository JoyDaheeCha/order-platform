package com.flab.orderplatform.payment.application.port.out;

import lombok.Builder;

/**
 * 결제 승인 요청
 * @param orderNumber 주문번호
 * @param amount 결제 총액
 */
@Builder
public record PgApprovalRequest(
        String orderNumber,
        Long amount
) {
}
