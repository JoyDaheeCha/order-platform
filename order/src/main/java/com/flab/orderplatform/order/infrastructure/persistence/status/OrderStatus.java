package com.flab.orderplatform.order.infrastructure.persistence.status;

/**
 * 주문 상태
 */
public enum OrderStatus {
    PENDING("결제 대기중"),
    PAID("결제 완료"),
    CONFIRMED("주문 확인 완료"),
    CANCELLED("주문 취소");

    final String description;

    OrderStatus(String description) {
        this.description = description;
    }
}
