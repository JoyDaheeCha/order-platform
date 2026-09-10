package com.flab.orderplatform.order.application.command;

import com.flab.orderplatform.order.domain.Order;

/**
 * 결제 후 타임아웃으로 인한 주문 실패 명령
 *
 */
public record OrderFailByPaymentTimeoutCommand(
        String orderNumber
) {
    public Order fail(Order order) {
        return order.failByTimeout();
    }
}
