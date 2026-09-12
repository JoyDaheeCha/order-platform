package com.flab.orderplatform.payment.domain.status;

import lombok.Getter;

/**
 * 결제 상태
 *
 * REQUESTED : 재호출해도 안전 (PG 연동전)
 * IN_PROGRESS: 재호출 금지. PG 호출로 결제 여부 확인후 FAILED or COMPLETED or REFUNDED로 변경 가능
 * FAILED, COMPLETED, REFUNDED: PG 처리 완료
 */
@Getter
public enum PaymentStatus {
    /**
     * 레코드는 생성되고 PG 미호출
     */
    REQUESTED("결제 대기"),
    /**
     * 결제 PG 호출됨. 결과 미확정
     */
    IN_PROGRESS("결제처리중"),
    COMPLETED("결제 완료"),
    FAILED("결제 실패"),

    /**
     * 환불 PG 호출됨. 결과 미확정
     */
    REFUND_IN_PROGRESS("환불처리중"),
    REFUNDED("환불 완료");

    private final String description;

    PaymentStatus(String description) {
        this.description = description ;
    }
}
