package com.flab.orderplatform.order.domain.status;

/**
 * 주문 실패 사유
 */
public enum OrderFailedReasonType {
    INVENTORY_SHORTAGE("재고 부족"),
    TIMEOUT("시간초과"),
    PAYMENT_FAIL("결제 실패");

    final String description;

    OrderFailedReasonType(String description) {
        this.description = description;
    }
}
