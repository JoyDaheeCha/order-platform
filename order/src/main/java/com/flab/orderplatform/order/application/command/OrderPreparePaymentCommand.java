package com.flab.orderplatform.order.application.command;

import com.flab.orderplatform.order.domain.Order;

/**
 * 주문 생성
 *
 */
public record OrderPreparePaymentCommand(
        String orderNumber
) {
    public Order preparePayment(Order order) {
        return order.preparePayment();
    }
}
