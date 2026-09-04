package com.flab.orderplatform.order.application.command;

import com.flab.orderplatform.order.domain.Order;

/**
 * 주문 생성
 *
 */
public record OrderFailByInventoryShortageCommand(
        String orderNumber
) {
    public Order fail(Order order) {
        return order.failByInventoryShortage();
    }
}
