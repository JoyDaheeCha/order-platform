package com.flab.orderplatform.order.application.command;

import com.flab.orderplatform.order.domain.Order;

public record OrderCancelCommand(
        String orderNumber
) {
    public Order cancel(Order order) {
        return order.cancel();
    }
}
