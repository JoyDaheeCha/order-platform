package com.flab.orderplatform.order.application.command;

import com.flab.orderplatform.order.domain.Order;

/**
 * 주문 완료 명령
 *
 */
public record OrderConfirmCommand(
        String orderNumber
) {
    public Order confirm(Order order) {
        return order.confirm();
    }
}
