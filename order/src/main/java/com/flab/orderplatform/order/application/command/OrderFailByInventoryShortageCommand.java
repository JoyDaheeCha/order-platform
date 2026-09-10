package com.flab.orderplatform.order.application.command;

import com.flab.orderplatform.order.domain.Order;

/**
 * 재고 부족으로 인한 주문 실패 명령
 *
 * @param orderNumber 주문번호
 */
public record OrderFailByInventoryShortageCommand(
        String orderNumber
) {
    public Order fail(Order order) {
        return order.failByInventoryShortage();
    }
}
