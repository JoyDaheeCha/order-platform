package com.flab.orderplatform.payment.application.port.out;

import java.util.UUID;

/**
 * PG사 환불 요청 응답값
 * @param tid PG사 결제 고유 번호
 * @param isSucceed PG사 요청 결과 (성공 or 실패)
 * @param message 환불 승인시 null & 결제 실패시 해당 사유
 */
public record PgRefundResult(
        String tid,
        boolean isSucceed,
        String message
) {
    public static PgRefundResult fail() {
        return new PgRefundResult(UUID.randomUUID().toString(), false, "한도 초과");
    }

    public static PgRefundResult success() {
        return new PgRefundResult(UUID.randomUUID().toString(), true, null);
    }
}
