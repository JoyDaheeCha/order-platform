package com.flab.orderplatform.order.domain.status;

/**
 * 주문 상태
 */
public enum OrderStatus {
    RESERVING_INVENTORY("재고 선점 중"),
    PENDING("결제 대기중"), // TODO : 네이밍 변경
    PAID("결제 완료"),
    CONFIRMED("주문 확인 완료"),
    CANCELLED("주문 취소"),
    ORDER_FAILED("주문 실패");

    final String description;

    OrderStatus(String description) {
        this.description = description;
    }
}
