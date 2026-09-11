package com.flab.orderplatform.order.application.command;

import com.flab.orderplatform.order.domain.Order;

import java.time.LocalDateTime;

/**
 * 주문 결제 준비 완료 명령
 *
 * @param orderNumber 주문 번호
 * @param reservedAt  재고 선점일시
 */
public record OrderPreparePaymentCommand(
        String orderNumber,
        LocalDateTime reservedAt
) {
    public Order preparePayment(Order order) {
        return order.preparePayment(reservedAt);
    }
}
