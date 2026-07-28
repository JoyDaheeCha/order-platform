package com.flab.orderplatform.payment.domain.status;

/**
 * 결제 상태
 */
public enum PaymentStatus {
    REQUESTED("결제 대기"),
    COMPLETED("결제 완료"),
    FAILED("결제 실패"),
    REFUNDED("환불 완료");

    private final String description;

    PaymentStatus(String description) {
        this.description = description ;
    }
}
