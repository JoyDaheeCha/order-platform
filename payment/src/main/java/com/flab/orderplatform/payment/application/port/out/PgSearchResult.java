package com.flab.orderplatform.payment.application.port.out;

/**
 * PG사 조회 응답값
 * @param tid PG사 결제 고유 번호
 * @param isPaid PG 사 결제 결과 (결제 성공: true, 결제 실패: false)
 */
public record PgSearchResult(
        String tid,
        boolean isPaid
) {
}
