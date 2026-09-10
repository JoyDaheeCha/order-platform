package com.flab.orderplatform.order.domain.status;

/**
 * 재고 선점 해제 사유
 */
public enum OrderInventoryReservationReleaseReason {
    TIMEOUT("재고 선점후 시간초과"),
    PAYMENT_COMPLETED("결제 성공"),
    PAYMENT_FAILED("결제 실패");

    final String description;

    OrderInventoryReservationReleaseReason(String description) {
        this.description = description;
    }
}
