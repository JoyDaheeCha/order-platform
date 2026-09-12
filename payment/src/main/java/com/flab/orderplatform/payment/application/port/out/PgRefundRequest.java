package com.flab.orderplatform.payment.application.port.out;

import lombok.Builder;

/**
 * 결제 환불 요청
 * @param pgTid 결제 데이터 PK
 */
@Builder
public record PgRefundRequest(
        Long pgTid
) {
}
